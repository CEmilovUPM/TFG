function registerUser() {
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const name = document.getElementById('name').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    if (password !== confirmPassword) {
        alert("Passwords do not match");
        return;
    }

    const requestBody = JSON.stringify({
        email: email,
        password: password,
        name: name,
        confirmPassword: confirmPassword
    });

    fetch('/auth/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: requestBody
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(data => {
                throw new Error(data.message || 'Something went wrong');
            });
        }
        return response.json();
    })
    .then(data => {
        console.log(data);
        alert("Registration successful!");
        // Optionally redirect to login page or clear the form
    })
    .catch(error => {
        console.error('Error:', error);
        alert("Error: " + error.message);
    });
}