# ATS Career Assistant

A powerful web application designed to help job seekers optimize their resumes for Applicant Tracking Systems (ATS). This tool parses a standard PDF resume, uses the Google Gemini AI to intelligently structure the data, generates a clean and ATS-friendly PDF, and can even create a custom cover letter tailored to a specific job description.

## 🚀 Key Features

-   **PDF Resume Parsing**: Extracts text content from any uploaded PDF resume.
-   **AI-Powered Data Structuring**: Leverages the Google Gemini API to convert unstructured resume text into a clean, organized JSON format.
-   **ATS-Friendly PDF Generation**: Creates a new, minimalist PDF from the structured data, ensuring maximum compatibility with modern ATS platforms.
-   **AI Cover Letter Generation**: On-demand, generates a compelling cover letter based on the user's resume data and a provided job description.
-   **Real-time Progress Updates**: Uses Server-Sent Events (SSE) to provide a seamless user experience, showing live updates as the documents are being processed.
-   **Bundled Downloads**: Packages the generated ATS-friendly resume and the cover letter into a single `.zip` file for easy downloading.

## 🛠️ Technology Stack

### Backend
-   **Java 17**
-   **Spring Boot 3**
-   **Spring AI** (for Gemini API integration)
-   **Apache PDFBox** (for PDF text extraction)
-   **Maven** (for dependency management)

### Frontend
-   **React**
-   **TypeScript**
-   **Fetch API** (for handling Server-Sent Events stream)

## 🏁 Getting Started

To get a local copy up and running, follow these simple steps.

### Prerequisites

Make sure you have the following installed on your machine:
-   **JDK 17** or later
-   **Maven 3.8** or later
-   **Node.js** and **npm**
-   A **Google Gemini API Key**. You can obtain one from [Google AI Studio](https://aistudio.google.com/app/apikey).

### Installation & Running

The project is structured into a backend (Spring Boot) and a frontend (React). You need to run both simultaneously.

#### 1. Backend (Spring Boot)

1.  **Clone the repository:**
    ```bash
    git clone <your-repository-url>
    ```

2.  **Navigate to the project directory:**
    ```bash
    cd ats-cv-converter
    ```

3.  **Configure the application:**
    -   Go to `src/main/resources/`.
    -   Open `application.properties`.
    -   Set the `cors.allowed.origins` property to your frontend's address.
    ```properties
    # The address where your React frontend is running
    cors.allowed.origins=http://localhost:3000
    ```
    *The Gemini API key is not set here; it is provided by the user through the frontend UI.*

4.  **Build the project:**
    This project contains unit tests that might fail if not configured properly. To build the application quickly by bypassing them, run the following command in your terminal:
    ```bash
    mvn clean install -DskipTests
    ```
    Alternatively, you can use the Maven panel in your IDE and activate the "Skip Tests" mode before running the `install` lifecycle goal.

5.  **Run the application:**
    You can run the application directly from your IDE (e.g., IntelliJ IDEA) by running the `AtsConverterApplication` class, or by using the following Maven command:
    ```bash
    mvn spring-boot:run
    ```
    The backend server will start on `http://localhost:8080`.

#### 2. Frontend (React)

1.  **Navigate to the frontend directory** (assuming it's a separate folder):
    ```bash
    cd path/to/your/frontend-folder
    ```

2.  **Install dependencies:**
    ```bash
    npm install
    ```

3.  **Run the application:**
    ```bash
    npm start
    ```
    The frontend development server will start, and your browser should automatically open to `http://localhost:3000`.

## ⚙️ How It Works

1.  **Upload CV**: The user uploads their resume in PDF format.
2.  **Provide API Key**: The user enters their Google Gemini API Key.
3.  **(Optional) Generate Cover Letter**: The user can choose to generate a cover letter by providing a job description.
4.  **Process**: The backend receives the file and initiates an asynchronous process:
    -   It extracts text from the PDF.
    -   It sends two separate requests to the Gemini API: one to structure the CV data and another to write the cover letter.
    -   It generates a new ATS-friendly PDF.
    -   It bundles the files into a `.zip` archive if a cover letter is requested.
    -   Throughout this process, it sends status updates to the frontend via SSE.
5.  **Download**: Once the process is complete, the frontend receives a final message with a unique download ID. It then redirects the browser to a download endpoint, initiating the file download.

## 📄 API Endpoints

-   `POST /api/v1/cv/generate-stream`: The main endpoint that accepts the PDF file, API key, and job description. It initiates the SSE connection and starts the document generation process.
-   `GET /api/v1/cv/download`: A simple endpoint that serves the generated file. It takes a `fileId` and `filename` as query parameters.