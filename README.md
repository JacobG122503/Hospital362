# Hospital-362

Hospital-362 is a comprehensive, text-based Java application designed to simulate and manage the day-to-day operations of a hospital. It provides a robust Command Line Interface (CLI) that serves various hospital staff roles, ensuring smooth coordination between medical services, pharmacy, facilities, and administration.

## 🏥 Features

The system is built around role-based workflows, ensuring that each staff member has access to the tools they need:

### 🩺 Medical Services (Doctors & Surgeons)
- **Prescribe Medication:** Draft and submit prescriptions with built-in checks for patient allergies and active medication conflicts.
- **Schedule Surgical Procedures:** Book operating rooms, assign surgeons, and prevent scheduling conflicts.

### 💊 Pharmacy (Pharmacists)
- **Dispense Medication:** Fulfill pending prescriptions for admitted patients based on real-time inventory.
- **Audit Inventory:** Review current stock levels against ideal quantities and generate restock reports.
- **Order Supplies:** Automatically identify understocked medications and generate purchase orders to replenish the pharmacy.

### 💉 Nursing (Nurses)
- **View Rooms:** Monitor patient room assignments and statuses.

### 🛠️ Facilities Management
- **Room & Equipment Management:** Oversee hospital rooms and medical equipment availability.
- **Process Cleaning Queue:** Manage and dispatch cleaning requests for hospital rooms after patient discharge or surgeries.

### 🏢 Administration & HR
- **Patient Management:** Admit new patients, update medical profiles, and handle discharges.
- **Employee Management:** Hire new staff and manage the hospital's workforce.
- **Applicant Screening:** Built-in blacklisting system for job applicants.

## 📂 Project Structure

The project follows a clean, service-oriented architecture:

- `src/Main.java`: The entry point and main menu loop of the application.
- `src/objects/`: Contains data models (`Patient`, `Employee`, `Surgery`, `Room`, etc.).
- `src/services/`: Contains the core business logic, separated by domain (`PharmacyService`, `SurgicalService`, `RoomService`, `HiringService`, etc.).
- `data/`: The local database consisting of CSV files used for data persistence (e.g., `patients.csv`, `pharmacy_inventory.csv`, `surgeries.csv`, `purchase_orders.csv`).

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK)** 11 or higher.

### Compilation
To compile the project, navigate to the root directory and run:

```bash
javac -d out $(find src -name "*.java")
```

### Execution
Run the compiled application using:

```bash
java -cp out Main
```

## 💾 Data Persistence

Hospital-362 uses CSV files for lightweight, reliable data storage. All data is stored in the `data/` directory. Upon the first run, the system will automatically initialize any missing database files (like `purchase_orders.csv` or `surgeries.csv`) to ensure seamless operation.
