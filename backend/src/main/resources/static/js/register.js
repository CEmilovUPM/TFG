// public/js/register.js

document.addEventListener('DOMContentLoaded', () => {
  const form             = document.getElementById('register-form');
  const nameInput        = document.getElementById('reg-name');
  const emailInput       = document.getElementById('reg-email');
  const passInput        = document.getElementById('reg-password');
  const confirmInput     = document.getElementById('reg-confirm-password');
  const nameErrDiv       = document.getElementById('name-error');
  const emailErrDiv      = document.getElementById('email-error');
  const passErrDiv       = document.getElementById('password-error');
  const confirmErrDiv    = document.getElementById('confirm-error');
  const generalErrDiv    = document.getElementById('general-error');

  function clearErrors() {
    [nameErrDiv, emailErrDiv, passErrDiv, confirmErrDiv, generalErrDiv]
      .forEach(d => { d.innerText = ''; d.style.color = ''; });
  }

  form.addEventListener('submit', async e => {
    e.preventDefault();
    clearErrors();

    const payload = {
      name:             nameInput.value.trim(),
      email:            emailInput.value.trim(),
      password:         passInput.value,
      confirmPassword:  confirmInput.value
    };

    try {
      const res  = await fetch('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();

      if (res.status === 201) {
        const at = data.info?.accessToken;
        const rt = data.info?.refreshToken;
        if (at && rt) {
          localStorage.setItem('accessToken', at);
          localStorage.setItem('refreshToken', rt);
          window.location.href = '/home';
        } else {
          generalErrDiv.style.color = 'red';
          generalErrDiv.innerText = 'Registered but no tokens returned.';
        }
        return;
      }

      // on error: map fields
      if (data.errors) {
        Object.entries(data.errors).forEach(([key, msg]) => {
          if (key.startsWith('name_')) {
            nameErrDiv.style.color = 'red';
            nameErrDiv.innerText = msg;
          } else if (key.startsWith('email_')) {
            emailErrDiv.style.color = 'red';
            emailErrDiv.innerText = msg;
          } else if (key === 'passwords_dont_match' || key.startsWith('confirm')) {
            confirmErrDiv.style.color = 'red';
            confirmErrDiv.innerText = msg;
          } else if (key.startsWith('password_')) {
            passErrDiv.style.color = 'red';
            passErrDiv.innerText = msg;
          } else {
            generalErrDiv.style.color = 'red';
            generalErrDiv.innerText += msg + '\n';
          }
        });
      }
      if (data.info) {
        if (data.info.email_already_registered) {
          emailErrDiv.style.color = 'red';
          emailErrDiv.innerText = data.info.email_already_registered;
        }
        if (data.info.message) {
          generalErrDiv.style.color = 'red';
          generalErrDiv.innerText += data.info.message;
        }
      }

    } catch (err) {
      console.error('Register error:', err);
      generalErrDiv.style.color = 'red';
      generalErrDiv.innerText = 'An error occurred. Please try again.';
    }
  });
});
