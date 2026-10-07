
document.getElementById("logform").addEventListener("submit", async function (e) {
    e.preventDefault(); // this line is what stops the page from reloading with ?email=...&password=...
  
    const form = e.target;
    const email = form.email.value.trim();
    const password = form.password.value;
  
    try {
      const response = await fetch("/api/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
      });
  
      const result = await response.json().catch(() => ({}));
  
      if (response.ok) {
        localStorage.setItem("ecorepair_user", JSON.stringify(result));
        
        if (result.role === "ADMIN") {
          window.location.href = "admin-dashboard.html";
        } else if (result.role === "COLLECTOR") {
          window.location.href = "collector-dashboard.html";
        } else {
          window.location.href = "user-dashboard.html";
        }

      } else {
        alert(result.message || "Login failed.");
      }
    } catch (error) {
      console.error(error);
      alert("Could not connect to the server.");
    }
  });