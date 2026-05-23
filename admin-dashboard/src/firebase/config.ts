import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getDatabase } from "firebase/database";
import { getStorage } from "firebase/storage";

// Extracted from app/google-services.json
const firebaseConfig = {
  apiKey: "AIzaSyAEQKDAGHeRG6gULhVKsHvFNza_HJ68kiI",
  authDomain: "uthsocial-a2f90.firebaseapp.com",
  databaseURL: "https://uthsocial-a2f90-default-rtdb.firebaseio.com",
  projectId: "uthsocial-a2f90",
  storageBucket: "uthsocial-a2f90.firebasestorage.app",
  messagingSenderId: "1072362430757",
  appId: "1:1072362430757:web:d44544479ee94fd129facc" // Constructed web ID based on project details
};

const app = initializeApp(firebaseConfig);

export const auth = getAuth(app);
export const db = getFirestore(app);
export const rtdb = getDatabase(app);
export const storage = getStorage(app);

export default app;
