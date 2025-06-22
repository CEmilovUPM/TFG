document.addEventListener('DOMContentLoaded', function () {

  const monthSelect = document.getElementById('monthSelect');
  const currentMonth = String(new Date().getMonth() + 1).padStart(2, '0');
  monthSelect.value = currentMonth;


  const yearSelect = document.getElementById('yearSelect');
  const currentYear = new Date().getFullYear();
  for (let y = currentYear - 5; y <= currentYear + 5; y++) {
    const option = document.createElement('option');
    option.value = y;
    option.textContent = y;
    if (y === currentYear) option.selected = true;
    yearSelect.appendChild(option);
  }

  function attachClickHandler() {
    const graphElement = document.querySelector('.plotly-graph-div');
    if (!graphElement || typeof graphElement.on !== 'function') return;

    graphElement.on('plotly_click', function(eventData) {
      if (!eventData?.points?.length) return;

      const point = eventData.points[0];
      const clickedDate = point.x;

      const redirectUrl = `/user/${window.userId}/goals/${window.goalId}?date=${clickedDate}`;
      window.location.href = redirectUrl;
    });
  }

  function updateGraph() {
    const year = document.getElementById('yearSelect').value;
    const month = document.getElementById('monthSelect').value;
    const selectedMonth = `${year}-${month}`;

    const url = new URL(window.location.href);
    url.searchParams.set('month', selectedMonth);
    url.searchParams.set('partial', 'true');

    fetch(url)
      .then(response => response.json())
      .then(data => {
        const config = {
          staticPlot: false,
          modeBarButtonsToRemove: ['lasso2d', 'select2d', 'zoom2d', 'zoomIn2d', 'zoomOut2d', 'autoScale2d', 'pan2d'],
          displayModeBar: false
        };

        data.layout = data.layout || {};
        data.layout.dragmode = false;
        data.layout.xaxis = data.layout.xaxis || {};
        data.layout.yaxis = data.layout.yaxis || {};
        data.layout.xaxis.fixedrange = true;
        data.layout.yaxis.fixedrange = true;

        const graphDiv = document.getElementById('graph-container');
        Plotly.react(graphDiv, data.data, data.layout, config);
        attachClickHandler();
      })
      .catch(error => {
        console.error('Error fetching updated graph:', error);
      });
  }

  document.getElementById('applyMonthBtn').addEventListener('click', updateGraph);
  attachClickHandler();
});