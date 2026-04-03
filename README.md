# Employee-Leave-Management-System
## 📌 Overview
The Employee Leave Management System is a Java-based application designed to manage employee leave requests efficiently. It allows employees to apply for leave, and administrators/managers to review, approve, or reject requests. The system helps maintain transparency, reduce paperwork, and automate leave tracking.
## 🚀 Features
* Employee registration and login
* Apply for leave (Casual, Sick, Paid, etc.)
* View leave history and status
* Admin/Manager dashboard
* Approve or reject leave requests
* Leave balance tracking
* Secure authentication system
## 🛠️ Technologies Used
* Programming Language: Java
* Frontend: Java Swing / JSP (depending on your implementation)
* Backend: Core Java / Servlets
* Database: MySQL / Oracle
* IDE: Eclipse / IntelliJ IDEA / NetBeans
## Screenshots
- Dashboard

## ⚙️ Installation & Setup
1. Clone the Repository
```bash
git clone https://github.com/your-username/employee-leave-management-system.git
```
2. Open in IDE
* Import the project into Eclipse / IntelliJ / NetBeans
3. Configure Database
* Create a database (e.g., leave_management)
* Import the SQL file (if provided)
* Update database credentials in the config file
4. Run the Application
* Run the main class (for desktop app)
* OR deploy on server (for web app)
## 🧑‍💻 Usage
### Employee
* Register/Login
* Apply for leave
* Check leave status
 ### Admin/Manager
 * Login
 * View all leave requests
 * Approve/Reject requests
 * Manage employees
## 🗄️ Database Tables
### Employee Table
* employee_id
* name
* email
* password
* role
### Leave Table
* leave_id
* employee_id
* leave_type
* start_date
* end_date
* status
## 🔒 Security Features
* Password authentication
* Role-based access control (Admin/Employee)
* Input validation
## 📈 Future Enhancements
* Email notifications
* Mobile app integration
* Leave analytics dashboard
* Role-based multi-level approval
## ⭐ Acknowledgements
* Open-source community
* Java documentation


