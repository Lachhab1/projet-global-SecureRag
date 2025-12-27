import React, { useState, useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

function ChatInterface() {
    const [messages, setMessages] = useState([
        { role: 'bot', text: 'Hello! I am your Secure RAG Assistant. Ask me about CVEs or Cyber Threats.' }
    ]);
    const [input, setInput] = useState('');
    const [loading, setLoading] = useState(false);
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(scrollToBottom, [messages]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!input.trim()) return;

        const userMessage = { role: 'user', text: input };
        setMessages(prev => [...prev, userMessage]);
        setInput('');
        setLoading(true);

        try {
            const response = await fetch('http://localhost:8080/api/rag/ask', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query: userMessage.text })
            });

            const data = await response.json();

            if (data.error) {
                setMessages(prev => [...prev, {
                    role: 'bot',
                    text: "⚠️ " + data.error,
                    isError: true
                }]);
            } else {
                setMessages(prev => [...prev, {
                    role: 'bot',
                    text: data.answer,
                    confidence: data.confidence,
                    sources: data.sources
                }]);
            }
        } catch (err) {
            setMessages(prev => [...prev, { role: 'bot', text: "Error connecting to server. Is backend running?", isError: true }]);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="chat-container">
            <div className="messages-area">
                {messages.map((msg, idx) => (
                    <div key={idx} className={`message ${msg.role}`}>
                        <div className={`message-content ${msg.role === 'bot' ? 'bot-content' : ''}`}>
                            {msg.role === 'bot' ? (
                                <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.text}</ReactMarkdown>
                            ) : (
                                msg.text
                            )}
                        </div>
                        {msg.role === 'bot' && !msg.isError && msg.confidence && (
                            <div className="bot-meta">
                                <span className={`chip ${msg.confidence}`}>Confidence: {msg.confidence}</span>
                                {msg.sources && msg.sources.length > 0 && (
                                    <span className="chip">Sources: {msg.sources.join(', ')}</span>
                                )}
                            </div>
                        )}
                    </div>
                ))}
                {loading && <div className="message bot"><p>Thinking...</p></div>}
                <div ref={messagesEndRef} />
            </div>

            <div className="input-area">
                <form className="input-box" onSubmit={handleSubmit}>
                    <input
                        type="text"
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        placeholder="Ask about a CVE or threat..."
                        disabled={loading}
                    />
                    <button type="submit" className="send-btn" disabled={loading}>
                        {loading ? '...' : '➤'}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default ChatInterface;
