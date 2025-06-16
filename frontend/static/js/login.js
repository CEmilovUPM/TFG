
document.addEventListener('DOMContentLoaded', () => {
  const form        = document.getElementById('login-form');
  const emailInput  = document.getElementById('login-email');
  const passInput   = document.getElementById('login-password');
  const responseDiv = document.getElementById('login-response');

  function clearMessage() {
    responseDiv.innerText = '';
    responseDiv.classList.add('d-none');
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();
    clearMessage();

    const email    = emailInput.value.trim();
    const password = passInput.value;

    if (!email || !password) {
      responseDiv.style.color = 'red';
      responseDiv.innerText = 'Email and password must not be empty.';
      return;
    }

    try {
      const res = await fetch('/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
        credentials: 'include'
      });

      const data = await res.json();

      if (res.ok) {
        const res_id = await fetch('/profile', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
        });

        const body_res = await res_id.json();
        if (body_res.data[0].isAdmin){
            window.location.href = `/admin`;
        }else{
            window.location.href = `/user/${body_res.data[0].id}/goals`;
        }

      } else {
        const msg = data.message
          || (data.errors ? Object.values(data.errors).join('\n') : 'Login failed.');
        responseDiv.classList.remove('d-none');
        responseDiv.classList.add('alert', 'alert-danger');
        responseDiv.innerText = msg;
      }
    } catch (err) {
      console.error('Login error:', err);
      responseDiv.style.color = 'red';
      responseDiv.innerText = 'An error occurred. Please try again.';
    }
  });
});