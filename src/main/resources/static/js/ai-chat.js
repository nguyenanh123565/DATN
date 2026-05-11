/**
 * SmartHome Frontend - AI Chat Module (Premium Version)
 * Tự động tích hợp Widget tư vấn AI vào mọi trang.
 */

(function() {
    let aiSessionId = sessionStorage.getItem('smartHomeAiSessionId');
    const API_CHAT_URL = '/api/ai/chat';

    // 0. Inject CSS
    const chatStyles = `
        .ai-widget-container { position: fixed; bottom: 30px; right: 30px; z-index: 9999; font-family: 'Inter', sans-serif; }
        .ai-widget-btn { 
            width: 60px; height: 60px; 
            background: linear-gradient(135deg, #6c63ff 0%, #48cae4 100%); 
            color: white; border-radius: 50%; 
            display: flex; align-items: center; justify-content: center; 
            font-size: 24px; cursor: pointer; 
            box-shadow: 0 10px 25px rgba(108, 99, 255, 0.4); 
            transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            position: relative;
        }
        .ai-widget-btn:hover { transform: scale(1.1) rotate(5deg); box-shadow: 0 15px 30px rgba(108, 99, 255, 0.5); }
        .badge-dot { 
            position: absolute; top: 5px; right: 5px; width: 12px; height: 12px; 
            background: #ff4d4d; border: 2px solid white; border-radius: 50%; 
        }
        .ai-chat-box { 
            position: absolute; bottom: 80px; right: 0; width: 360px; height: 500px; 
            background: #fff; border-radius: 20px; box-shadow: 0 15px 50px rgba(0,0,0,0.2); 
            display: none; flex-direction: column; overflow: hidden; border: 1px solid rgba(0,0,0,0.05);
            transition: width 0.3s ease, height 0.3s ease;
        }
        .ai-chat-box.open { display: flex; animation: slideUp 0.4s ease; }
        .ai-chat-box.expanded { width: 500px; height: 70vh; max-width: 95vw; max-height: 800px; }
        .ai-chat-header { 
            background: linear-gradient(135deg, #6c63ff 0%, #48cae4 100%); 
            padding: 18px; color: white; display: flex; align-items: center; justify-content: space-between; 
        }
        .ai-chat-messages { flex: 1; overflow-y: auto; padding: 15px; background: #f8f9fa; display: flex; flex-direction: column; gap: 10px; }
        .ai-msg { padding: 10px 14px; border-radius: 15px; max-width: 85%; font-size: 14px; line-height: 1.5; }
        .ai-msg.bot { background: white; align-self: flex-start; color: #333 !important; border-bottom-left-radius: 2px; box-shadow: 0 2px 5px rgba(0,0,0,0.05); }
        .ai-msg.user { background: #6c63ff; color: white !important; align-self: flex-end; border-bottom-right-radius: 2px; }
        .ai-chat-input-area { padding: 15px; display: flex; gap: 10px; background: white; border-top: 1px solid #eee; }
        .ai-chat-input { flex: 1; border: 1px solid #eee; padding: 10px 15px; border-radius: 25px; outline: none; font-size: 14px; color: #333 !important; background: white !important; }
        .ai-chat-input:focus { border-color: #6c63ff; }
        .ai-send-btn { width: 40px; height: 40px; background: #6c63ff; color: white; border: none; border-radius: 50%; cursor: pointer; transition: 0.2s; }
        .ai-send-btn:hover { background: #48cae4; transform: scale(1.1); }
        @keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
        .typing-indicator span { height: 8px; width: 8px; background: #bbb; display: inline-block; border-radius: 50%; margin: 0 2px; animation: bounce 1.3s infinite; }
        .typing-indicator span:nth-child(2) { animation-delay: 0.15s; }
        .typing-indicator span:nth-child(3) { animation-delay: 0.3s; }
        @keyframes bounce { 0%, 60%, 100% { transform: translateY(0); } 30% { transform: translateY(-4px); } }
    `;

    function injectStyles() {
        const styleTag = document.createElement('style');
        styleTag.innerHTML = chatStyles;
        document.head.appendChild(styleTag);
    }

    // 1. Tự động thêm HTML vào Body nếu chưa có
    function injectChatbotHTML() {
        injectStyles();
        if (document.getElementById('ai-widget')) return;

        const widgetHTML = `
            <div id="ai-widget" class="ai-widget-container">
                <div class="ai-widget-btn" id="ai-widget-btn">
                    <i class="fa-solid fa-robot"></i>
                    <span class="badge-dot"></span>
                </div>
                <div class="ai-chat-box" id="ai-chat-box">
                    <div class="ai-chat-header">
                        <h5><i class="fa-solid fa-microchip"></i> SmartHome Advisor</h5>
                        <div>
                            <i class="fa-solid fa-expand ai-expand-btn fs-5 me-3" id="ai-expand-btn" style="cursor:pointer;" title="Phóng to/Thu nhỏ"></i>
                            <i class="fa-solid fa-xmark ai-close-btn fs-4" id="ai-close-btn" style="cursor:pointer;" title="Đóng"></i>
                        </div>
                    </div>
                    <div class="ai-chat-messages" id="ai-chat-messages">
                        <div class="ai-msg bot">
                            Xin chào! 👋 Tôi là trợ lý AI của SmartHome. Tôi có thể giúp bạn tìm kiếm và tư vấn các thiết bị nhà thông minh phù hợp. Bạn đang quan tâm đến sản phẩm nào?
                        </div>
                    </div>
                    <div class="ai-chat-input-area">
                        <input type="text" id="ai-chat-input" class="ai-chat-input" placeholder="Nhập câu hỏi của bạn...">
                        <button class="ai-send-btn" id="ai-send-btn">
                            <i class="fa-solid fa-paper-plane"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', widgetHTML);
    }

    // 2. Khởi tạo Logic
    function initChatbot() {
        const aiBtn = document.getElementById('ai-widget-btn');
        const aiBox = document.getElementById('ai-chat-box');
        const aiCloseBtn = document.getElementById('ai-close-btn');
        const aiExpandBtn = document.getElementById('ai-expand-btn');
        const aiSendBtn = document.getElementById('ai-send-btn');
        const aiInput = document.getElementById('ai-chat-input');
        const aiMessages = document.getElementById('ai-chat-messages');

        if (!aiBtn || !aiBox) return;

        // Toggle Box
        aiBtn.addEventListener('click', () => {
            aiBox.classList.toggle('open');
            if (aiBox.classList.contains('open')) {
                aiInput.focus();
                // Ẩn badge dot khi mở
                const dot = aiBtn.querySelector('.badge-dot');
                if (dot) dot.style.display = 'none';
            }
        });

        aiCloseBtn.addEventListener('click', () => {
            aiBox.classList.remove('open');
        });

        if (aiExpandBtn) {
            aiExpandBtn.addEventListener('click', () => {
                aiBox.classList.toggle('expanded');
                if (aiBox.classList.contains('expanded')) {
                    aiExpandBtn.classList.remove('fa-expand');
                    aiExpandBtn.classList.add('fa-compress');
                } else {
                    aiExpandBtn.classList.remove('fa-compress');
                    aiExpandBtn.classList.add('fa-expand');
                }
                scrollToBottom();
            });
        }

        // Handle Send
        aiSendBtn.addEventListener('click', sendMessage);
        aiInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') sendMessage();
        });

        function appendMessage(text, type, products = null, skipSync = false) {
            // Xoá typing indicator nếu có
            const existingTyping = document.getElementById('ai-typing');
            if (existingTyping) existingTyping.remove();

            const msgDiv = document.createElement('div');
            msgDiv.className = `ai-msg ${type}`;
            
            if (text === 'typing') {
                msgDiv.innerHTML = `<div class="typing-indicator"><span></span><span></span><span></span></div>`;
                msgDiv.id = 'ai-typing';
            } else {
                // Tối ưu: Nếu có sản phẩm cards, chúng ta có thể làm sạch text (bỏ phần list đánh số thủ công nếu Backend gửi kèm)
                let cleanText = text;
                if (products && products.length > 0) {
                    // Xóa các dòng bắt đầu bằng "1. ", "2. " v.v. để tránh lặp với Card
                    cleanText = text.split('\n').filter(line => !/^\d+\.\s/.test(line.trim())).join('\n');
                }

                // Parse markdown đơn giản (chỉ chuyển đổi **text** thành <strong>)
                const formattedText = cleanText.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
                                               .replace(/\n/g, '<br>');
                msgDiv.innerHTML = formattedText;
                
                // Thêm sản phẩm gợi ý nếu có (Giao diện Premium Card)
                if (products && products.length > 0) {
                    const cardsContainer = document.createElement('div');
                    cardsContainer.className = 'ai-product-cards mt-2 d-flex overflow-auto pb-2';
                    cardsContainer.style.gap = '10px';
                    
                    products.forEach(p => {
                        const card = document.createElement('div');
                        card.className = 'ai-product-card flex-shrink-0 bg-white rounded p-2 text-dark shadow-sm';
                        card.style.width = '140px';
                        card.style.cursor = 'pointer';
                        card.style.fontSize = '0.85rem';
                        
                        // Xử lý ảnh
                        let imageUrl = 'https://placehold.co/100x100?text=No+Img';
                        try {
                            if (p.imageUrls || p.productImage) {
                                let raw = p.imageUrls || p.productImage;
                                if (raw.startsWith('[')) {
                                    imageUrl = JSON.parse(raw)[0];
                                } else {
                                    imageUrl = raw;
                                }
                            }
                        } catch(e) {}

                        card.innerHTML = `
                            <img src="${imageUrl}" class="w-100 rounded mb-1" style="height:80px;object-fit:cover">
                            <div class="fw-bold text-truncate" title="${p.name || p.productName}">${p.name || p.productName}</div>
                            <div class="text-primary small">${typeof formatCurrency === 'function' ? formatCurrency(p.salePrice || p.price || p.unitPrice) : (p.salePrice || p.price || p.unitPrice) + 'đ'}</div>
                        `;
                        card.onclick = () => window.location.href = `/product-detail.html?id=${p.id}`; 
                        cardsContainer.appendChild(card);
                    });
                    msgDiv.appendChild(cardsContainer);
                }
            }

            aiMessages.appendChild(msgDiv);
            scrollToBottom();

            // Đồng bộ với Inline Chat trên trang Tư vấn nếu có
            if (!skipSync && window.tuvanInlineChat) {
                window.tuvanInlineChat.addInlineBubble(text, type, products, true);
            }
        }

        function scrollToBottom() {
            aiMessages.scrollTop = aiMessages.scrollHeight;
        }

        async function sendMessage() {
            const text = aiInput.value.trim();
            if (!text) return;

            aiInput.value = '';
            appendMessage(text, 'user');
            
            // Delay một chút rồi hiện typing
            setTimeout(() => {
                appendMessage('typing', 'bot');
            }, 300);

            try {
                // Sử dụng axios trực tiếp để linh hoạt với API path
                const response = await axios.post(API_CHAT_URL, {
                    message: text,
                    sessionId: aiSessionId
                });

                const data = response.data;
                if (data.sessionId) {
                    aiSessionId = data.sessionId;
                    sessionStorage.setItem('smartHomeAiSessionId', aiSessionId);
                }
                
                // Giả lập thời gian chờ để AI "suy nghĩ"
                setTimeout(() => {
                    appendMessage(data.reply, 'bot', data.suggestedProducts);
                }, 500);

            } catch (err) {
                console.error('AI Chat Error:', err);
                appendMessage('Rất tiếc, tôi đang mất kết nối với hệ thống. Vui lòng thử lại sau hoặc gọi hotline **0823422987** nhé! 🔌', 'bot');
            }
        }
        // Load history nếu có session
        if (aiSessionId) {
            axios.get(`/api/ai/history/${aiSessionId}`)
                .then(res => {
                    if (res.data && res.data.messages && res.data.messages.length > 0) {
                        aiMessages.innerHTML = ''; // Xóa câu chào mặc định
                        res.data.messages.forEach(msg => {
                            const role = msg.role === 'USER' ? 'user' : 'bot';
                            appendMessage(msg.content, role);
                        });
                    }
                })
                .catch(err => console.error("Could not load chat history", err));
        }

        // Xuất hàm để đồng bộ
        window.aiChatWidget = {
            appendMessage: appendMessage,
            setSessionId: (id) => { 
                aiSessionId = id; 
                sessionStorage.setItem('smartHomeAiSessionId', id); 
            }
        };
    }

    // Chạy khi DOM sẵn sàng
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            injectChatbotHTML();
            initChatbot();
        });
    } else {
        injectChatbotHTML();
        initChatbot();
    }
})();
