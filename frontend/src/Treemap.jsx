import { useEffect, useRef, useState } from "react";
import * as d3 from "d3";

export default function Treemap({ repoId }) {
    const ref = useRef();
    const [path, setPath] = useState(null);
    const [data, setData] = useState([]);
    const [breadcrumbs, setBreadcrumbs] = useState([]);
    const [summary, setSummary] = useState(null);

    useEffect(() => {
        loadLevel(null);
        loadSummary();
    }, [repoId]);

    function loadLevel(p) {
        const q = p ? `?path=${p}` : "";
        fetch(`/api/treemap/${repoId}/level${q}`)
            .then(r => r.json())
            .then(d => {
                setData(d);
                setPath(p);
                setBreadcrumbs(p ? p.split("/") : []);
            });
    }

    function loadSummary() {
        fetch(`/api/metrics/summary/${repoId}`)
            .then(r => r.json())
            .then(d => {
                console.log("Данные Summary получены:", d);
                setSummary(d);
            });
    }

    useEffect(() => {
        draw();
    }, [data]);

    function draw() {
        if (!data || data.length === 0) return;
        const width = 900, height = 500;



        d3.select(ref.current).selectAll("*").remove();
        const svg = d3.select(ref.current)
            .attr("width", width)
            .attr("height", height);

        const root = d3.hierarchy({ children: data })
            .sum(d => d.value || 1)
            .sort((a, b) => (b.value || 0) - (a.value || 0));

        d3.treemap().size([width, height]).padding(2)(root);

        const nodes = svg.selectAll("g")
            .data(root.leaves())
            .enter()
            .append("g")
            .attr("transform", d => `translate(${d.x0},${d.y0})`);

        nodes.append("rect")
            .attr("width", d => d.x1 - d.x0)
            .attr("height", d => d.y1 - d.y0)
            .attr("fill", d => d3.interpolateReds(d.data.risk || 0))
            .attr("stroke", "#333")
            .style("cursor", "pointer")
            .on("click", (_, d) => {
                if (d.data.hasChildren) {
                    const newPath = path ? `${path}/${d.data.name}` : d.data.name;
                    loadLevel(newPath);
                }
            });

        nodes.append("title")
            .text(d => {
                const riskVal = ((d.data.risk || 0) * 100).toFixed(1);
                return `Файл: ${d.data.name}\nChurn: ${d.data.value}\nRisk: ${riskVal}%`;
            });

        nodes.append("text")
            .attr("x", 5)
            .attr("y", 15)
            .text(d => (d.x1 - d.x0 > 60) ? d.data.name : "")
            .attr("font-size", "11px")
            .attr("fill", "#000")
            .style("pointer-events", "none");
    }

    return (
        <div style={{ padding: "20px", background: "#1a1a1a", minHeight: "100vh", color: "#fff", fontFamily: "sans-serif" }}>
            <h2 style={{ textAlign: "center" }}>Аналитика репозитория</h2>

            <div style={{ display: "flex", gap: "20px", marginBottom: "30px", justifyContent: "center" }}>
                <div style={cardStyle}>
                    <div style={labelStyle}>🛡️ Bus Factor</div>
                    <div style={valueStyle}>{summary?.busFactor ?? "0"}</div>
                </div>
                <div style={cardStyle}>
                    <div style={labelStyle}>❤️ Health Score</div>
                    <div style={valueStyle}>{summary?.healthScore ?? "0"}%</div>
                </div>
                <div style={cardStyle}>
                    <div style={labelStyle}>⚠️ Статус</div>
                    <div style={{ fontSize: "14px", marginTop: "10px", color: "#ffa500" }}>
                        {summary?.burnoutAlert || "Все стабильно"}
                    </div>
                </div>
            </div>

            <div style={{ marginBottom: "15px", textAlign: "center" }}>
                <button onClick={() => loadLevel(null)} style={btnStyle}>root</button>
                {breadcrumbs.map((b, i) => (
                    <button key={i} onClick={() => loadLevel(breadcrumbs.slice(0, i + 1).join("/"))} style={btnStyle}>
                        / {b}
                    </button>
                ))}
            </div>

            <div style={{ display: "flex", justifyContent: "center" }}>
                <svg ref={ref} style={{ borderRadius: "8px", border: "1px solid #333" }}></svg>
            </div>
        </div>
    );
}

const cardStyle = { background: "#2d2d2d", padding: "20px", borderRadius: "12px", width: "250px", textAlign: "center", border: "1px solid #444" };
const labelStyle = { color: "#aaa", fontSize: "14px", marginBottom: "8px" };
const valueStyle = { fontSize: "32px", fontWeight: "bold" };
const btnStyle = { background: "#444", color: "#fff", border: "none", padding: "8px 15px", margin: "0 5px", borderRadius: "6px", cursor: "pointer" };