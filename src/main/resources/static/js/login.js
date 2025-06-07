
document.getElementById("loginForm").addEventListener("submit", function(event) {
    event.preventDefault();

    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    // Create the login data object
    const loginData = {
        email: email,
        password: password
    };

    // Send the login request as JSON
    fetch('/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json', // Tell the server we're sending JSON
        },
        body: JSON.stringify(loginData), // Convert the data to JSON format
    })
    .then(response => response.json())
    .then(data => {
        if (data.accessToken) {
            console.log("Login successful!", data);
            // Store token, navigate, etc.
        } else {
            console.error("Login failed", data);
        }
    })
    .catch(error => {
        console.error('Error:', error);
    });
});
