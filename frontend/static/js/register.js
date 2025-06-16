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
    .forEach(d => {
      d.innerText = '';
      d.style.color = '';
      d.classList.add('d-none');
    });
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
      const res  = await fetch('/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });
      const data = await res.json();

      if (res.status === 201) {
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
        return;
      }

      // on error: map fields
      if (data.errors) {
        Object.entries(data.errors).forEach(([key, msg]) => {
          if (key.startsWith('name_')) {
            nameErrDiv.style.color = 'red';
            nameErrDiv.innerText = msg;
            nameErrDiv.classList.remove('d-none');
          } else if (key.startsWith('email_')) {
            emailErrDiv.style.color = 'red';
            emailErrDiv.innerText = msg;
            emailErrDiv.classList.remove('d-none');
          } else if (key === 'passwords_dont_match' || key.startsWith('confirm')) {
            confirmErrDiv.style.color = 'red';
            confirmErrDiv.innerText = msg;
            confirmErrDiv.classList.remove('d-none');
          } else if (key.startsWith('password_')) {
            passErrDiv.style.color = 'red';
            passErrDiv.innerText = msg;
            passErrDiv.classList.remove('d-none');
          } else {
            generalErrDiv.style.color = 'red';
            generalErrDiv.innerText += msg + '\n';
            generalErrDiv.classList.remove('d-none');

          }
        });
      }
      if (data.info) {
        if (data.info.email_already_registered) {
          emailErrDiv.style.color = 'red';
          emailErrDiv.innerText = data.info.email_already_registered;
          emailErrDiv.classList.remove('d-none');
        }
        if (data.info.message) {
          generalErrDiv.style.color = 'red';
          generalErrDiv.innerText += data.info.message;
          generalErrDiv.classList.remove('d-none');
        }
      }

    } catch (err) {
      console.error('Register error:', err);
      generalErrDiv.style.color = 'red';
      generalErrDiv.innerText = 'An error occurred. Please try again.';
      generalErrDiv.classList.remove('d-none');
    }
  });
});