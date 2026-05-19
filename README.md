# Emergency Service Dispatch Prioritization System

An interactive, GUI-based simulation designed to optimize emergency service dispatches (Ambulance, Fire, and Police) using core algorithmic paradigms. This system ensures critical, high-priority emergencies are handled first while minimizing overall delay using multiple sorting techniques.

---

## 👥 Authors
* **Student 1:** (Your Name) — `1RV25MC052`
* **Student 2:** (Teammate Name) — `1RV25MC053`
* **Course:** Master of Computer Applications (MCA), Semester 2
* **Subject:** Analysis & Design of Algorithms (ADA) - Experiential Learning Project

---

## 🚀 Core Features
* **Multi-Criteria Prioritization:** Sorts service requests by **Priority Level** (Critical ➔ High ➔ Medium ➔ Low) and breaks ties using the lowest **Estimated Response Time**.
* **Algorithmic Adaptability:** Switch seamlessly between three distinct algorithms to view execution speeds and structural step changes.
* **Real-Time Visualisation:** Watch the arrays/heaps alter configurations dynamically using an animated playback bar layout with custom speed sliders.
* **Robust Dispatch Logs:** Generates precise console printouts mapping execution time directly down to the nanosecond (`ns`).

---

## 🧠 Algorithmic Framework

| Technique | Approach / Paradigm | Time Complexity (Best/Worst) | Space Complexity |
| :--- | :--- | :--- | :--- |
| **Priority Queue (Max-Heap)** | Greedy Extraction | $O(n \log n)$ / $O(n \log n)$ | $O(n)$ |
| **Merge Sort** | Divide & Conquer (Stable) | $O(n \log n)$ / $O(n \log n)$ | $O(n)$ |
| **Quick Sort** | Divide & Conquer (Random Pivot) | $O(n \log n)$ / $O(n^2)$ | $O(\log n)$ |

---

## 💻 How to Run the Application Locally

Ensure you have **Java Development Kit (JDK)** installed on your machine.

### 1. Clone the Repository
```bash
git clone [https://github.com/YOUR_USERNAME/ADA_EL-main.git](https://github.com/YOUR_USERNAME/ADA_EL-main.git)
cd ADA_EL-main
```

### 2. Compile the Source File
```bash
javac EmergencyDispatch.java
```

### 3. Execute the Program
```bash
java EmergencyDispatch

---
```
