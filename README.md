# Vaultapp 🔐

Vaultapp is a **secure authentication and authorization backend** built with **Spring Boot**. It demonstrates real‑world security patterns including **JWT authentication, OAuth2 login (Google)**, refresh tokens, and account protection mechanisms.

This project is designed as a **production‑style auth system** suitable for modern frontend applications (React, Vue, mobile apps).

---

## 🚀 Features

* ✅ **JWT Authentication** (stateless access tokens)
* 🔁 **Refresh Token System**
* 🔐 **OAuth2 Login (Google)** with account linking
* 📧 **Email verification support**
* 🚫 **Failed login tracking & account locking**
* 🔄 **Custom OAuth2 user service**
* 🛡️ **Spring Security configuration**
* 👤 **Provider‑aware users (LOCAL / GOOGLE / etc.)**

---

## 🧠 Architecture Overview

* **Spring Boot** – core application framework
* **Spring Security** – authentication & authorization
* **OAuth2 Client** – Google OAuth login
* **JWT** – access token generation & validation
* **Refresh Tokens** – persistent session handling
* **JPA / Hibernate** – data persistence
* **MySQL / PostgreSQL (configurable)** – database

Authentication Flow:

1. User logs in via **email/password** or **Google OAuth**
2. Backend validates credentials or OAuth provider
3. User is created or linked if necessary
4. Backend issues **JWT access token + refresh token**
5. Frontend uses JWT for secured API access

---

## 🔑 OAuth2 Flow (Google)

* Custom `CustomOAuth2UserService` handles:

  * Provider identification
  * Email verification checks
  * Existing account linking
  * New OAuth user creation

* On success:

  * `OAuth2SuccessHandler` issues JWT + refresh token
  * Redirects frontend with tokens

---

## 🧪 How to Run Locally

### Prerequisites

* Java 17+
* Maven
* MySQL or PostgreSQL

### Steps

```bash
git clone https://github.com/tobintonye/Vaultapp.git
cd Vaultapp
mvn spring-boot:run
```

Set environment variables for:

* Database credentials
* JWT secret
* OAuth2 client ID & secret

---

## 📁 Project Structure

```
Vaultapp
├── Config        # Security, OAuth2, JWT configuration
├── Model         # User, RefreshToken, enums
├── Repository    # JPA repositories
├── Services      # JWT & token services
└── Controller    # Auth endpoints
```

---

## 🧩 Technologies Used

* Java
* Spring Boot
* Spring Security
* OAuth2 Client
* JWT
* Hibernate / JPA
* MySQL / PostgreSQL
* Maven

---

## 📌 Resume‑Ready Highlights

* Designed and implemented a **secure authentication system** using Spring Security
* Integrated **OAuth2 (Google login)** with account linking
* Built **JWT + refresh token** based authentication for stateless APIs
* Implemented security features such as **email verification, failed login tracking, and account locking**

---

## 📈 Future Improvements

* Add unit & integration tests
* Add role‑based authorization (RBAC)
* Add email service integration
* Dockerize application
* Deploy to cloud (AWS / Railway / Render)

---

## 👤 Author

**Tonye Tobin**
Backend Engineer | Java & Spring Boot

GitHub: [https://github.com/tobintonye](https://github.com/tobintonye)
