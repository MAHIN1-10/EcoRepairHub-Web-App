
// Show the admin access code field only when "Admin" is selected
document.querySelectorAll('input[name="role"]').forEach((radio) => {
  radio.addEventListener("change", () => {
    document.getElementById("adminCodeGroup").style.display =
      document.querySelector('input[name="role"]:checked').value === "ADMIN" ? "block" : "none";
  });
});

document.getElementById("userform").addEventListener("submit", async function (e) {
  e.preventDefault();

  const form = e.target;

  const fullname = form.fullname.value.trim();
  const email = form.email.value.trim();
  const password = form.password.value;
  const confirm = form.confirm.value;

  if (password !== confirm) {
    alert("Passwords do not match!");
    return;
  }
  if (password.length < 8) {
    alert("Password must be at least 8 characters.");
    return;
  }

  const submitBtn = form.querySelector("button[type='submit']");
  submitBtn.disabled = true;

  const role = form.role.value;
  const adminCode = form.adminCode.value;
  if (role === "ADMIN" && !adminCode) {
    alert("Please enter the admin access code.");
    submitBtn.disabled = false;
    return;
  }

  const userData = { fullname, email, password, confirm, role, adminCode };

  try {
    const response = await fetch("/api/saveData",{
    
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(userData)
    });

    const result = await response.json().catch(() => ({}));

    if (response.ok) {
      alert(result.message || "Registration successful!");
      window.location.href = "Login.html";
    } else {
      alert(result.message || "Registration failed.");
    }
  } catch (error) {
    console.error(error);
    alert("Could not connect to the server. Is it running on port 8082?");
  } finally {
    submitBtn.disabled = false;
  }
});