# **User Manual: ATM Simulator System**

## **1. Introduction**

The ATM Simulator System is designed to handle user transactions such as deposits, withdrawals, and transfers while
considering owed amounts. The system is built with extensibility in mind, allowing future upgrades and improvements with
minimal changes.

---

## **2. Commands**

* `login [name]` - Logs in as this customer and creates the customer if not exist
* `deposit [amount]` - Deposits this amount to the logged in customer
* `withdraw [amount]` - Withdraws this amount from the logged in customer
* `transfer [target] [amount]` - Transfers this amount from the logged in customer to the target customer
* `logout` - Logs out of the current customer
* `exit` - Exit

---

## **3. Assumptions**

- The **maximum length for a username** is **64 characters**,
- User's name is case-sensitive.
- Commands that do not require arguments will **ignore any extra arguments** provided.
- The **amount field supports up to 15 integer digits and two decimal places**, assuming transactions occur in **USD or
  SGD**.
- The **console logs can be enabled or disabled** based on configuration settings.
- **More than two users** are supported in the system.
- The system **clears data on start-up**, but persistence can be enabled through application properties.
- The **H2 console** can be used to visualize user details, active users, balances, and owed amounts.

---

## **4. Design Considerations**

- The solution is designed for **easy extension** and maintainability.
- The **current version is single-threaded**, meaning concurrent transactions are not handled.
    - A **simple fix** for concurrency is using **synchronized transaction methods**.
    - Future upgrades will allow multiple active users to perform transactions simultaneously.
- The solution can be **extended to support REST APIs** with modifications to controller classes.
- The system is designed to be **database-agnostic**, allowing integration with different relational databases by
  changing application properties.

---

## **5. Extensibility & Future Enhancements**

### **5.1 Extending System Capabilities**

- Support for **REST APIs** by modifying controller classes.
- Persistence can be enabled to avoid data loss **by storing information in a database**.
- Multi-threaded support to handle **concurrent transactions efficiently**.
- Support for additional **currencies** by allowing currency selection per transaction.

### **5.2 Enhancements for Transactions**

- **Better tracking** of:
    - Transfer details
    - Owed-to details
    - Owed-from details
- **More complex nested debt scenarios**, e.g., A owes B, B owes C, and so on.
- **Refactoring deposit and transfer services** to extract common logic and improve maintainability.

### **5.3 Testing & Debugging Improvements**

- **Integration tests currently do not cover CLI-based execution**, but this can be enhanced.
- Improved **deposit and transfer handling** for complex owed amount chains.
- Using **H2 Console** to debug and analyze user balances and transactions visually.

---

## **6. Summary**

The ATM Simulator System is a flexible and extensible application that allows deposits, transfers, and owed amount
tracking. It has been designed with future enhancements in mind, including multi-threading, REST API support, database
persistence, and improved debt tracking.

Future work can focus on improving concurrency, expanding test coverage, refining business logic, and making transaction
tracking more efficient.