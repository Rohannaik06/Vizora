
# Vizora | Premium Eyewear E-Commerce Platform 🕶️
> A production-grade, full-stack enterprise e-commerce web application engineered for high-performance eyewear retail, featuring real-time inventory management, advanced multi-stage order tracking, and secure payment gateway integration.

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-F80000?style=for-the-badge&logo=jakartaee&logoColor=white)
![Apache Tomcat](https://img.shields.io/badge/Apache%20Tomcat-F8DC75?style=for-the-badge&logo=apache-tomcat&logoColor=black)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Razorpay](https://img.shields.io/badge/Razorpay-0C2340?style=for-the-badge&logo=razorpay&logoColor=white)
![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)

---

## 📑 Table of Contents
* [Overview](#-overview)
* [Key Features & Architecture](#-key-features--architecture)
* [Tech Stack](#-tech-stack)
* [System Workflow & Order Lifecycle](#-system-workflow--order-lifecycle)
* [Project Preview](#-project-preview)
* [Getting Started & Local Setup](#-getting-started--local-setup)
* [API & Servlet Routing Reference](#-api--servlet-routing-reference)
* [Author](#-author)

---

## 🚀 Overview
**Vizora** is a comprehensive, feature-rich e-commerce web application built from scratch to simulate a real-world retail platform. It transitions away from simplistic mock applications by implementing robust server-side business logic, transactional database synchronization, secure multi-tier authentication, cryptographic payment verification via **Razorpay**, and a dedicated **Enterprise Admin Control Dashboard**.

Designed with scalability and user experience in mind, Vizora handles everything from dynamic cart processing and real-time inventory stock deductions to multi-status order tracking (Confirmed, Packed, Shipped, Out for Delivery, Delivered).

---

## 💡 Key Features & Architecture

### **🛍️ Customer Experience & Storefront**
* **Dynamic Product Catalog:** Filter eyewear by categories (Sunglasses, Computer Glasses, Frames) and target demographics with live stock status indicators.
* **Smart Shopping Cart & Direct Checkout:** Seamlessly transition items from cart persistence or execute a direct "Buy Now" flow.
* **Interactive Wishlist Engine:** Instant toggle support for saving and managing favorite products with animated UI states.
* **Automated Price Calculations:** Real-time calculation of MRP, tiered discounts, item counts, and zero-charge logistics.

### **💳 Secure Multi-Mode Checkout & Payment Gateway**
* **Razorpay Integration:** Complete cryptographic HMAC-SHA256 signature verification for online transactions (UPI, Credit/Debit cards, NetBanking).
* **Cash on Delivery (COD):** Alternative placement flow with immediate order confirmation and inventory reservation.
* **Automated Stock De-escalation:** Instant synchronization reducing product inventory counts upon successful transaction completion to prevent race conditions or overselling.

### **📦 Advanced Order Tracking & History**
* **5-Stage Visual Progress Bar:** Real-time order visualization tracking items across: *Confirmed ➔ Packed ➔ Shipped ➔ Out for Delivery ➔ Delivered*.
* **Smart Cancellation Constraints:** Customers can safely cancel orders dynamically while in *Confirmed* or *Packed* states.
* **Detailed Breakdown:** Order cards displaying payment tags, payment IDs (for online orders), estimated 3-day delivery timelines, and multi-criteria searching/filtering.

### **🛡️ Enterprise Admin Control Dashboard**
* **Metrics & Analytics:** Centralized management system monitoring total platform revenue, inventory levels, and user directories.
* **Granular Order Status Control:** Admin panel optimized for rapid status updates, allowing administrators to push items through packing, shipping, and out-for-delivery phases.
* **Theme Switching:** Full native dark/light mode toggle built into the administrative layout for ergonomic workflow management.

---

## 🛠 Tech Stack
* **Backend:** Java (Servlets, JDBC, Servlet Filters), Jakarta EE (`jakarta.servlet.*`)
* **Database:** MySQL relational database featuring optimized table joins (`orders`, `order_items`, `products`, `cart`, `users`)
* **Server & Deployment:** Apache Tomcat 10.1.x (Enterprise-grade servlet container)
* **Payment Processing:** Razorpay Java SDK & Web Checkout API (HMAC-SHA256 signature verification)
* **Frontend:** HTML5, Modern CSS3 (Grid, Flexbox, Custom Properties/Variables), Vanilla ES6+ JavaScript, FontAwesome 6 Icons, Google Fonts (Poppins & Inter)

---

## 🔄 System Workflow & Order Lifecycle


```

[Customer Checkout]
│
├──► [Online Payment via Razorpay] ──► [HMAC Signature Verification] ──┐
│                                                                      ▼
└──► [Cash on Delivery (COD)] ──────────────────────────────► [Database Persistence]
│
┌──────────────────────────────────────┘
▼
[Stock De-escalation] (-1 per item)
│
▼
[Order Status: CONFIRMED]
│
▼
[Admin Dashboard Status Progression]
(Confirmed ➔ Packed ➔ Shipped ➔ Out for Delivery ➔ Delivered)

```

---

## 📸 Project Preview

### **Storefront & Customer Journey**
| Storefront Home | Secure Checkout | Order Tracking & History |
| :---: | :---: | :---: |
| *Responsive grid layout with dynamic category filters and wishlist toggles* | *Multi-step accordion checkout with address preview and payment selection* | *5-step visual tracking progress bar with live status badges* |

### **Enterprise Admin Management**
| Admin Analytics Dashboard | Enterprise Orders Management | Product Catalog Control |
| :---: | :---: | :---: |
| *Metrics overview and system monitoring* | *Master orders table with status dropdown actions* | *Inventory level adjustments and stock tracking* |

*(Note: High-resolution interface mockups and system architecture diagrams are organized inside the `/Screenshots` directory.)*

---

## 🚀 Getting Started & Local Setup

### **Prerequisites**
* Java Development Kit (JDK 17 or higher)
* Apache Tomcat Server (v10.1.x)
* MySQL Server (v8.0+)
* An IDE supporting Java Web Projects (Eclipse IDE Enterprise Edition, IntelliJ IDEA Ultimate, or VS Code with Tomcat extensions)

### **Step-by-Step Installation**
1. **Clone the Repository:**
   ```bash
   git clone [https://github.com/rohannaik06/Vizora-Ecommerce.git](https://github.com/rohannaik06/Vizora-Ecommerce.git)

```

2. **Database Configuration:**
* Open MySQL Workbench or your preferred SQL client.
* Create a database named `VIZORA`:
```sql
CREATE DATABASE VIZORA;
USE VIZORA;

```


* Run your database schema script to generate the required tables (`users`, `products`, `cart`, `orders`, `order_items`, `admin`).


3. **Configure Database Connection:**
* Verify your `DBConnection.java` file utilizes the correct local credentials:
```java
String url = "jdbc:mysql://localhost:3306/VIZORA?useSSL=false";
String user = "root";
String password = "your_mysql_password";

```




4. **Configure Payment Gateway Keys:**
* Insert your live or test Razorpay API Key and Secret inside `VerifyPaymentServlet.java` and `CreateRazorpayOrderServlet.java`.


5. **Deploy to Apache Tomcat:**
* Import the project into your Java IDE as a Dynamic Web Project / Maven web project.
* Export or configure the project directory under your Tomcat `webapps/VIZORA` path.
* Start the Apache Tomcat server (`http://localhost:8080/VIZORA/`).



---

## 🔌 API & Servlet Routing Reference

| Endpoint | Method | Description |
| --- | --- | --- |
| `/VIZORA/OrderServlet` | `POST` | Processes direct and cart checkout forms, generates order records, and reduces stock. |
| `/VIZORA/OrderHistoryServlet` | `GET` | Fetches comprehensive user order logs joined with product metadata as JSON. |
| `/VIZORA/CancelOrderServlet` | `POST` | Safely revokes active orders based on current fulfillment states. |
| `/VIZORA/CreateRazorpayOrderServlet` | `POST` | Initializes a secure server-side order with Razorpay parameters. |
| `/VIZORA/VerifyPaymentServlet` | `POST` | Executes HMAC-SHA256 cryptographic validation of incoming webhooks/responses. |
| `/VIZORA/AdminOrdersServlet` | `GET/POST` | Manages enterprise order queries and updates fulfillment milestones. |


---

## 👨‍💻 Author
**Rohan Naik** | [LinkedIn](https://www.linkedin.com/in/rohannaik06) | [Email](mailto:rohannaik1426@gmail.com)

*Developed as a high-performance enterprise e-commerce demonstration project showcasing advanced Java Web Technologies and Relational Database Management.*
