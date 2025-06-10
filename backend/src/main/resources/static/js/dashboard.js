document.addEventListener("DOMContentLoaded", () => {
  fetchGoals();
  document.getElementById("goal-form").addEventListener("submit", createGoal);
});

function fetchGoals() {
  fetch("/goals", {
    method: "GET",
    credentials: "include"
  })
    .then(res => {
      if (!res.ok) throw new Error("Failed to load goals");
      return res.json();
    })
    .then(json => {
      const goals = json.data || [];
      renderGoals(goals);
    })
    .catch(err => {
      console.error(err);
      document.getElementById("goals-container").innerHTML = "<p>Error loading goals.</p>";
    });
}

function renderGoals(goals) {
  const container = document.getElementById("goals-container");
  container.innerHTML = "";

  if (goals.length === 0) {
    container.innerHTML = "<p>You can begin by adding a goal.</p>";
    return;
  }

  goals.forEach(goal => {
    const goalDiv = document.createElement("div");
    goalDiv.classList.add("goal-item"); // optional wrapper class

    goalDiv.innerHTML = `
      <div class="goal-container">
        <div class="goal-info">
          <h2 style="margin-top: 0; margin-bottom: 8px;">${goal.title}</h2>
          <p style="margin:4px 0;">${goal.description}</p>
          <p style="margin:4px 0;"><strong>Metric:</strong> ${goal.metric}</p>
          <p style="margin:4px 0;"><strong>Target:</strong> ${goal.totalDesiredAmount}</p>
          <p style="margin:4px 0;"><small><strong>Created:</strong> ${
            new Date(goal.creationDate).toLocaleDateString(undefined, {
              year: 'numeric', month: 'short', day: 'numeric'
            })
          }</small></p>
        </div>
        <div class="goal-actions">
          <button class="view-progress">📊 View</button>
          <button class="update-goal" title="Edit">✏ Edit</button>
          <button class="delete-goal" title="Delete">🗑️ Del</button>
        </div>
      </div>
      <div class="progress-container" style="display: none; margin-top: 10px;"></div>
    `;
    container.appendChild(goalDiv);

    // Attach listeners
    goalDiv.querySelector(".view-progress").addEventListener("click", () => {
      const progressDiv = goalDiv.querySelector(".progress-container");
      if (progressDiv.style.display === "none") {
        fetchProgress(goal.id, progressDiv);
        progressDiv.style.display = "block";
      } else {
        progressDiv.style.display = "none";
      }
    });

    goalDiv.querySelector(".update-goal").addEventListener("click", () => {
      showUpdateForm(goalDiv, goal);
    });

    goalDiv.querySelector(".delete-goal").addEventListener("click", () => {
      deleteGoal(goal.id);
    });
  });
}

function fetchProgress(goalId, container) {
  container.innerHTML = "Loading progress...";
  fetch(`/goals/${goalId}/progress`, {
    method: "GET",
    credentials: "include"
  })
    .then(res => res.json())
    .then(data => {
      const progressList = (data.data || []).sort((a, b) => new Date(b.date) - new Date(a.date));
      if (progressList.length === 0) {
        container.innerHTML = "<p>No progress yet.</p>";
        return;
      }
      const ul = document.createElement("ul");
      progressList.forEach(p => {
        const li = document.createElement("li");
        li.innerHTML = `<strong>${p.amount}</strong> on ${p.date} - ${p.updateNote || "No note"}`;
        ul.appendChild(li);
      });
      container.innerHTML = "";
      container.appendChild(ul);
    })
    .catch(err => {
      console.error(err);
      container.innerHTML = "<p>Error loading progress.</p>";
    });
}

function createGoal(e) {
  e.preventDefault();
  const form = e.target;
  const data = {
    title: form.title.value,
    description: form.description.value,
    metric: form.metric.value,
    totalDesiredAmount: parseFloat(form.totalDesiredAmount.value)
  };

  fetch("/goals", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data)
  })
    .then(res => {
      if (!res.ok) throw new Error("Failed to create goal");
      return res.json();
    })
    .then(() => {
      form.reset();
      fetchGoals();
    })
    .catch(err => console.error("Create goal error:", err));
}

function deleteGoal(goalId) {
  if (!confirm("Are you sure you want to delete this goal?")) return;

  fetch(`/goals/${goalId}`, {
    method: "DELETE",
    credentials: "include"
  })
    .then(res => {
      if (!res.ok && res.status !== 204) throw new Error("Failed to delete goal");
      fetchGoals();
    })
    .catch(err => console.error("Delete goal error:", err));
}

function showUpdateForm(goalDiv, goal) {
  let existingForm = goalDiv.querySelector(".update-form");

  if (existingForm) {
    existingForm.style.display = existingForm.style.display === "none" ? "block" : "none";
    existingForm.querySelector("button").focus();
    return;
  }

  const formHtml = `
    <form class="update-form" autocomplete="off">
      <input type="text" name="title" value="${goal.title}" placeholder="Title" autocomplete="off" />
      <input type="text" name="description" value="${goal.description}" placeholder="Description" autocomplete="off" />
      <input type="text" name="metric" value="${goal.metric}" placeholder="Metric" autocomplete="off" />
      <input type="number" name="totalDesiredAmount" value="" step="0.01" placeholder="Target Amount" autocomplete="off" />
      <button type="submit">Save</button>
    </form>
  `;

  goalDiv.insertAdjacentHTML("beforeend", formHtml);
  const form = goalDiv.querySelector(".update-form");

  form.addEventListener("submit", (e) => {
    e.preventDefault();

    const getValue = (input) => {
      const val = input.value.trim();
      return val === "" ? null : val;
    };

    const data = {
      title: getValue(form.title),
      description: getValue(form.description),
      metric: getValue(form.metric),
      totalDesiredAmount: form.totalDesiredAmount.value.trim() === ""
        ? null
        : parseFloat(form.totalDesiredAmount.value)
    };

    fetch(`/goals/${goal.id}`, {
      method: "PATCH",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    })
      .then(res => {
        if (!res.ok) throw new Error("Failed to update goal");
        fetchGoals();
      })
      .catch(err => console.error("Update error:", err));
  });

  form.querySelector("button").focus();
}