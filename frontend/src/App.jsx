import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import Treemap from "./Treemap";

function App() {
    const [id, setId] = useState(1);

    return (
        <div>
            <div style={{ padding: "10px", textAlign: "center", background: "#333" }}>
                <span>ID репозитория: </span>
                <input
                    type="number"
                    value={id}
                    onChange={(e) => setId(e.target.value)}
                    style={{ width: "50px", background: "#444", color: "#fff", border: "1px solid #666" }}
                />
            </div>
            <h2>Project Treemap</h2>
            <Treemap repoId={id} />
        </div>
    );
}

export default App;
