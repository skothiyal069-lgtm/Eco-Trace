# 🌿 EcoTrace: 3D Supply Chain Transparency

EcoTrace is an interactive, 3D web application that brings radical transparency to the global supply chain. By simply scanning a product's barcode with your webcam, you can trace its journey from raw material extraction to retail checkout, complete with carbon footprint estimations and simulated blockchain verification.

## ✨ Key Features

- **📷 Live Barcode Scanning:** Integrated `html5-qrcode` allows users to scan products directly from their device's webcam.
- **🌍 Real Global Data:** Connects to the **Open Food Facts API** to fetch real-world product names, brands, origins, and packaging data.
- **⛓️ Simulated Blockchain:** Generates deterministic `sha256` transaction hashes for each stage of the supply chain, simulating an immutable ledger.
- **📉 Carbon Footprint Tracking:** Calculates estimated CO2 emissions and assigns a dynamic Eco Rating (A, B, or C) based on the product's journey.
- **🧊 3D Interactive UI:** 
  - Features a stunning **WebGL particle network background** (powered by Vanta.js) that dynamically changes color based on the product's Eco Rating.
  - Utilizes **VanillaTilt.js** for smooth, glare-enabled 3D hover effects on glassmorphism cards.
  - Implements **Cyberpunk text-scrambling** and sequential CSS animations for a premium feel.

## 🛠️ Tech Stack

- **Frontend:** Vanilla HTML, CSS (Glassmorphism + 3D Perspectives), JavaScript
- **Backend:** Node.js, Express.js
- **Libraries:** Axios, Three.js, Vanta.js, VanillaTilt, html5-qrcode
- **APIs:** Open Food Facts API

## 🚀 How to Run Locally

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/your-repo-name.git
   cd your-repo-name
   ```

2. **Install dependencies:**
   ```bash
   npm install
   ```

3. **Start the server:**
   ```bash
   node server.js
   ```

4. **Open your browser:**
   Navigate to `http://localhost:3000` to start scanning!

## 💡 How it works
If a valid grocery barcode is scanned, the Node.js backend fetches the exact product data from Open Food Facts. If the barcode is not in the public registry (e.g., non-food items), the backend mathematically generates a deterministic, realistic supply chain profile based on the barcode digits. 

---
*Built for a greener future.*
