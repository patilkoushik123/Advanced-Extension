let lastSelectedText = '';

document.addEventListener('DOMContentLoaded', () => {
  chrome.storage.local.get(['researchNotes'], function (result) {
    if (result.researchNotes) {
      document.getElementById('notes').value = result.researchNotes;
    }
  });

  document.getElementById('summarizeBtn').addEventListener('click', () => {
    console.log("Summarize button clicked");
    handleAIRequest('summarize');
  });

  document.getElementById('mcqBtn').addEventListener('click', () => {
    console.log("MCQ button clicked");
    handleAIRequest('generate-mcqs');
  });

  document.getElementById('saveNotesBtn').addEventListener('click', saveNotes);
  document.getElementById('downloadNotesBtn').addEventListener('click', downloadNotes);
  document.getElementById('downloadMcqsBtn').addEventListener('click', downloadResults);
  document.getElementById('toggleTheme').addEventListener('click', toggleTheme);
  document.getElementById('askQuestionBtn').addEventListener('click', askCustomQuestion);

  loadTheme();
});

async function handleAIRequest(operation) {
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      function: () => window.getSelection().toString()
    });

    if (result?.trim()) lastSelectedText = result.trim();

    if (!lastSelectedText) {
      showResult('⚠ Please select some content first.');
      return;
    }

    const response = await fetch('http://localhost:8081/api/research/process', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: lastSelectedText, operation })
    });

    if (!response.ok) throw new Error('API Error: ' + response.status);

    const text = await response.text();
    showResult(text.replace(/\n/g, '<br>'));
  } catch (error) {
    showResult('❌ ERROR: ' + error.message);
  }
}

function showResult(content) {
  document.getElementById('results').innerHTML = `
    <div class='result-item'>
      <div class="result-content">${content}</div>
    </div>
  `;
}

function saveNotes() {
  const notes = document.getElementById('notes').value;
  chrome.storage.local.set({ 'researchNotes': notes }, () => {
    alert('✅ Notes saved successfully.');
  });
}

function downloadFile(filename, content) {
  const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

function downloadNotes() {
  const notes = document.getElementById('notes').value;
  if (!notes.trim()) return alert("⚠ Notes are empty.");
  downloadFile("ResearchNotes.txt", notes);
}

function downloadResults() {
  const resultContent = document.getElementById('results').innerText;
  if (!resultContent.trim()) return alert("⚠ No MCQ or Summary result to download.");
  downloadFile("AI_Response.txt", resultContent);
}

function toggleTheme() {
  document.body.classList.toggle('dark');
  localStorage.setItem('theme', document.body.classList.contains('dark') ? 'dark' : 'light');
}

function loadTheme() {
  const theme = localStorage.getItem('theme');
  if (theme === 'dark') document.body.classList.add('dark');
}

async function askCustomQuestion() {
  const questionText = document.getElementById('customQuestion').value.trim();

  if (!questionText) {
    showResult('⚠ Please type a question first.');
    return;
  }

  try {
    const response = await fetch('http://localhost:8081/api/research/process', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: questionText, operation: 'custom-question' })
    });

    if (!response.ok) throw new Error('API Error: ' + response.status);

    const answer = await response.text();
    showResult(answer.replace(/\n/g, '<br>'));
  } catch (err) {
    showResult('❌ ERROR: ' + err.message);
  }
}
