document.addEventListener('DOMContentLoaded', function () {
  const yearSelect = document.getElementById('yearSelect');
  const monthSelect = document.getElementById('monthSelect');
  const applyBtn = document.getElementById('applyMonthBtn');
  const graphContainer = document.getElementById('graph-container');

  // Initialize year select (same as before)
  const currentYear = new Date().getFullYear();
  for (let y = currentYear - 5; y <= currentYear + 5; y++) {
    const option = document.createElement('option');
    option.value = y;
    option.textContent = y;
    if (y === currentYear) option.selected = true;
    yearSelect.appendChild(option);
  }
  const currentMonth = String(new Date().getMonth() + 1).padStart(2, '0');
  monthSelect.value = currentMonth;

  function fetchAndRenderGraph(yearMonth) {
    // Notice we call the same endpoint, but add partial=true
    const url = `/user/${window.userId}/goals/${window.goalId}/graph?month=${yearMonth}&partial=true`;

    fetch(url)
      .then(res => {
        if (!res.ok) throw new Error('Failed to fetch graph JSON');
        return res.json();
      })
      .then(graphJson => {
        const config = {
          staticPlot: false,
          modeBarButtonsToRemove: [
            'lasso2d', 'select2d', 'zoom2d',
            'zoomIn2d', 'zoomOut2d', 'autoScale2d', 'pan2d'
          ],
          displayModeBar: false
        };

        Plotly.react(graphContainer, graphJson.data, {
          ...graphJson.layout,
          yaxis: {
            ...graphJson.layout.yaxis,
            fixedrange: true,
            range: graphJson.layout.yaxis?.range || [0, 1]
          },
          xaxis: {
            ...graphJson.layout.xaxis,
            fixedrange: true,
          },
          dragmode: false,
        }, config);

        graphContainer.on('plotly_click', (eventData) => {
          if (!eventData.points || eventData.points.length === 0) return;
          const clickedDate = eventData.points[0].x;
          window.location.href = `/user/${window.userId}/goals/${window.goalId}?date=${clickedDate}`;
        });
      })
      .catch(err => {
        console.error(err);
        graphContainer.innerHTML = `<p class="text-danger">Failed to load graph data.</p>`;
      });
  }

  // Initial load
  fetchAndRenderGraph(`${yearSelect.value}-${monthSelect.value}`);

  applyBtn.addEventListener('click', () => {
    fetchAndRenderGraph(`${yearSelect.value}-${monthSelect.value}`);
  });
});
