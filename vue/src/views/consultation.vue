<template>
    <div class="consultation-container">
        <div class="sidebar">
            <!-- AI助手信息 -->
            <div class="ai-assistant-info">
                <div class="breathing-circle">
                    <el-image :src="iconUrl" alt="AI助手" style="width: 25px; height: 25px;" />
                </div>
                <h3 class="assistant-name">心理健康AI助手</h3>
                <div class="online-status">
                    <div class="status-dot"></div>
                    <span>在线服务中</span>
                </div>
            </div>
            <!-- 情绪花园 -->
             <div class="emotion-garden">
                <div class="garden-header">
                    <div class="garden-title">情绪花园</div>
                </div>
                <div class="emotion-info">
                    <div class="emotion-name">{{ currentEmotion.primaryEmotion }}</div>
                    <div class="emotion-score">{{ currentEmotion.emotionScore }}</div>
                </div>
                <div class="warm-tips">
                    <div class="emotion-status-text">
                        <span class="status-label">今天感觉</span>
                        <span class="status-emotion">{{ currentEmotion.isNegative ? '需要关注' : '还不错' }}</span>
                    </div>
                    <div class="emotion-intensity">
                        <span class="intensity-dots">
                            <span v-for="dot in 3" :key="dot" class="dot" :class="{'active': getIntensityClass(currentEmotion.emotionScore) >= dot}"></span>
                        </span>
                        <span class="intensity-text">{{ getRiskText(currentEmotion.riskLevel) }}</span>
                    </div>
                    <!-- 温暖建议卡片 -->
                    <div class="warm-suggestion" v-if="currentEmotion.suggestion">
                        <div class="suggestion-icon">💝</div>
                        <div class="suggestion-content">
                            <div class="suggestion-title">给你的小建议</div>
                            <div class="suggestion-text">{{ currentEmotion.suggestion }}</div>
                        </div>
                    </div>
                    <!-- 治愈行动 -->
                    <div class="healing-actions" v-if="currentEmotion.improvementSuggestions.length > 0">
                        <div class="actions-title">治愈小行动</div>
                        <div class="actions-list">
                            <div class="action-item" v-for="action in currentEmotion.improvementSuggestions" :key="action">
                                <div class="action-icon">✨</div>
                                <div class="action-text">{{ action }}</div>
                            </div>
                        </div>
                    </div> 
                    <!-- 风险提示 -->
                    <div class="risk-notice" v-if="currentEmotion.isNegative && currentEmotion.riskLevel > 1">
                        <div class="notice-icon">🤗</div>
                        <div class="notice-content">
                            <div class="notice-title">温馨提示</div>
                            <div class="notice-text">{{ currentEmotion.riskDescription }}</div>
                        </div>
                    </div>
                </div>
             </div>
            <!-- 会话列表 -->
            <div class="session-history">
                <h4 class="section-title">会话历史</h4>
                <div class="session-list">
                    <div v-for="session in sessionList" :key="session.id" @click="handleSessionClick(session)"
                        class="session-item">
                        <div class="session-info">
                            <div class="session-title">
                                <span>{{ session.sessionTitle }}</span>
                                <div class="session-meta">
                                    <span>{{ session.startAt }}</span>
                                </div>
                                <div class="session-preview">
                                    {{ session.lastMessageContent }}
                                </div>
                                <div class="session-stats">
                                    <span>
                                        <el-icon>
                                            <ChatRound />
                                        </el-icon>
                                        {{ session.messageCount || 0 }}
                                    </span>
                                    <span>
                                        <el-icon>
                                            <Clock />
                                        </el-icon>
                                        {{ session.duration || 0 }} 分钟
                                    </span>
                                </div>
                            </div>
                            <div class="session-actions">
                                <el-button text type="danger" size="mini" @click="handleDeleteSession(session.id)">
                                    <el-icon>
                                        <DeleteFilled />
                                    </el-icon>
                                </el-button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="chat-main">
            <div class="chat-header">
                <div class="header-left">
                    <div class="chat-avatar">
                        <el-image :src="iconUrl1" alt="AI助手" style="width: 30px; height: 30px;" />
                    </div>
                    <div class="chat-info">
                        <h2>心理健康AI助手</h2>
                        <p>您的心理健康助手</p>
                    </div>
                </div>
                <el-button circle @click="createNewFrontendSession" title="新建会话" size="large">
                    <el-icon>
                        <Plus />
                    </el-icon>
                </el-button>
            </div>
            <!-- 聊天消息区 -->
            <div class="chat-messages" ref="messagesContainer">
                <!-- 欢迎用语 -->
                <div v-if="messages.length === 0" class="message-item ai-message">
                    <div class="message-avatar">
                        <el-image :src="iconUrl" alt="AI助手" style="width: 18px; height: 18px;" />
                    </div>
                    <div class="message-content">
                        <div class="message-bubble">
                            <p>您好，我是小暖，您的AI心理助手，为您提供心理支持，请问有什么可以帮助您的吗？</p>
                        </div>
                        <div class="message-time">刚刚</div>
                    </div>
                </div>
                <!-- 消息列表 -->
                <div v-for="message in messages" :key="message.id" class="message-item"
                    :class="message.senderType === 2 ? 'ai-message' : 'user-message'">
                    <div class="message-avatar">
                        <el-image v-if="message.senderType === 2" style="width: 18px; height: 18px;" :src="iconUrl"
                            alt="AI助手" />
                        <el-image v-if="message.senderType === 1" style="width: 18px; height: 18px;" :src="userAvatar"
                            alt="用户" />
                    </div>
                    <div class="message-content">
                        <div class="message-bubble">
                            <!-- AI 正在思考，还没有内容 -->
                            <div class="typing-indicator"
                                v-if="message.senderType === 2 && isAiTyping && !message.content">
                                <div class="typing-dot"></div>
                                <div class="typing-dot"></div>
                                <div class="typing-dot"></div>
                            </div>
                            <!-- AI 流式输出中，显示已有内容 -->
                            <div v-else-if="message.senderType === 2 && isAiTyping && message.content"
                                class="streaming-content">
                                {{ message.content }}<span class="cursor-blink">|</span>
                            </div>
                            <!-- AI错误提示 -->
                            <div v-else-if="message.isError" class="error-message">
                                {{ message.content }}
                            </div>
                            <!-- AI正常消息（输出完成） -->
                            <MarkdownRenderer v-else-if="message.senderType === 2 && !message.isError"
                                :content="message.content" isAiMessage="true" />
                            <!-- 用户消息 -->
                            <p v-else-if="message.content">{{ message.content }}</p>
                        </div>
                        <div class="message-time">
                            {{ message.senderType === 2 && isAiTyping ? "正在输入中" : message.createdAt }}
                        </div>
                    </div>
                </div>
            </div>
            <!-- 聊天输入区 -->
            <div class="chat-input">
                <div class="input-container">
                    <el-input v-model="userMessage" placeholder="请输入您想要分享的内容" type="textarea" rows="3"
                        :disabled="isAiTyping" @keyup.enter="sendMessage" />
                        <div class="input-footer">
                            <span>按Enter发送,Shift+Enter换行</span>
                            <span>{{ userMessage.length }}/ 500</span>
                        </div>
                </div>
                <el-button class="send-btn" type="primary" @click="sendMessage" :disabled="!userMessage.trim() || userMessage.length > 500 || isAiTyping">
                    <el-icon>
                        <Promotion />
                    </el-icon>
                </el-button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { getSessionList, deleteSession, getSessionDetail, getSessionEmotion } from '@/api/frontend'
import { ElMessage } from 'element-plus'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'

const iconUrl = new URL('@/assets/images/robot.svg', import.meta.url).href
const iconUrl1 = new URL('@/assets/images/like.png', import.meta.url).href
const userAvatar = new URL('@/assets/images/users.png', import.meta.url).href

//新建会话
const createNewFrontendSession = () => {
    //创建新的会话对象
    const newSession = {
        sessionId: `temp_${Date.now()}`,
        status: 'TEMP',
        sessionTitle: '新对话',
    }
    currentSession.value = newSession
    //清空聊天记录
    messages.value = []
    InitEmotion()
}

//定义当前会话对象
const currentSession = ref(null)
const sessionList = ref([])

//定义对话消息
const messages = ref([])
//定义用户输入
const userMessage = ref('')
//定义AI是否正在输入
const isAiTyping = ref(false)

//初始化情绪花园
const currentEmotion = ref({})
const InitEmotion = () => {
    currentEmotion.value = {
        primaryEmotion: '中性',
        emotionScore: 50,
        isNegative: false,
        riskLevel: 0,
        suggestion: '情绪状态平稳',
        improvementSuggestions: [],
        riskDescription: ''
    }
}
InitEmotion()

const loadSessionEmotion = (sessionId) => {
    //确保sessionId格式正确
    const id = sessionId.toString().startsWith('session_') ? sessionId : `session_${sessionId}`

    getSessionEmotion(id).then(res =>{
        currentEmotion.value = res
    })
}

const getIntensityClass =  (score) =>{
    switch(true){
        case score >= 61:
            return 3
        case score >= 31:
            return 2
        default:
            return 1
    }
}

const getRiskText = (level) =>{
    switch(level){
        case 0:
            return '正常'
        case 1:
            return '需要关注'
        case 2:
            return '预警'
        case 3:
            return '危机'
        default:
            return '正常'
    }
}



// 消息列表容器，用于自动滚动
const messagesContainer = ref(null)

// 监听消息变化，自动滚动到底部
watch(messages, async () => {
    await nextTick()
    scrollToBottom()
})

// 监听 isAiTyping 结束后的消息更新
watch(isAiTyping, async () => {
    await nextTick()
    scrollToBottom()
})

const scrollToBottom = () => {
    if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
}


onMounted(() => {
    //获取会话列表
    getSessionPage()
    //初始化创建一个新对话
    createNewFrontendSession()
})

const sendMessage = () => {
    if (!userMessage.value.trim()) return

    if (isAiTyping.value) {
        ElMessage.error('AI助手正在输入中,请稍后')
        return
    }

    const message = userMessage.value.trim()
    userMessage.value = ''

    // 先添加用户消息到列表
    messages.value.push({
        id: `user_${Date.now()}`,
        senderType: 1,
        content: message,
        createdAt: new Date().toISOString(),
    })
    scrollToBottom()

    // 直接调用流式聊天接口（后端会处理会话创建）
    startAIStream(currentSession.value.sessionId, message, currentSession.value.sessionTitle)
}

const startAIStream = async (sessionId, message, sessionTitle) => {
    isAiTyping.value = true

    const aiMsgId = `ai_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
    messages.value.push({
        id: aiMsgId,
        senderType: 2,
        content: '',
        createdAt: new Date().toISOString(),
    })

    const aiMessage = messages.value[messages.value.length - 1]
    await nextTick()
    scrollToBottom()

    try {
        const response = await fetch('/api/psychological-chat/session/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Token': localStorage.getItem('token'),
                'Accept': 'text/event-stream',
            },
            body: JSON.stringify({
                sessionId: sessionId,
                message: message,
                sessionTitle: sessionTitle,
            }),
        })

        if (!response.ok) {
            handleStreamError(aiMessage)
            return
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
            const { done, value } = await reader.read()
            if (done) break

            buffer += decoder.decode(value, { stream: true })

            // 解析 SSE data: 行
            // 格式: data:CONTENT\n\n
            let lineEnd
            while ((lineEnd = buffer.indexOf('\n')) !== -1) {
                const line = buffer.slice(0, lineEnd).trim()
                buffer = buffer.slice(lineEnd + 1)

                // 只处理 data: 开头的行
                if (!line.startsWith('data:')) continue

                const data = line.slice(5).trim()
                if (!data) continue

                if (data === '[DONE]') {
                    isAiTyping.value = false
                    getSessionPage()
                    if (currentSession.value?.sessionId) {
                        loadSessionEmotion(currentSession.value.sessionId)
                    }
                    break
                } else if (data.startsWith('SESSION:')) {
                    const newSessionId = data.slice(8)
                    if (currentSession.value && currentSession.value.status === 'TEMP') {
                        currentSession.value.sessionId = newSessionId
                        currentSession.value.status = 'ACTIVE'
                    }
                } else if (data.startsWith('error:')) {
                    aiMessage.content = data.slice(6)
                    aiMessage.isError = 1
                    isAiTyping.value = false
                    ElMessage.error(data.slice(6))
                } else {
                    aiMessage.content += data
                    messages.value = [...messages.value]
                }

                // yield 给 Vue 更新 DOM
                await new Promise(resolve => setTimeout(resolve, 0))
            }
        }
    } catch (error) {
        handleStreamError(aiMessage)
    }

    isAiTyping.value = false
}

const handleStreamError = (aiMessage) => {
    if (aiMessage && !aiMessage.content) {
        aiMessage.content = 'AI回复失败，请重试'
    }
    isAiTyping.value = false
    ElMessage.error('AI回复失败，请稍后重试')
}

const getSessionPage = () => {
    getSessionList({
        pageNum: 1,
        pageSize: 10,
    }).then(res => {
        sessionList.value = res.records
    })
}


//获取会话数据
const handleSessionClick = (session) => {
    // 更新当前会话
    currentSession.value = {
        sessionId: session.id,
        status: 'ACTIVE',
        sessionTitle: session.sessionTitle,
    }
    //点击对话时,获取会话详情
    getSessionDetail(session.id).then(res => {
        messages.value = res
    })
    loadSessionEmotion(session.id)
}

//删除会话
const handleDeleteSession = (sessionId) => {
    deleteSession(sessionId).then(() => {
        ElMessage.success('会话删除成功')
        //刷新会话列表
        getSessionPage()
    })
}
</script>

<style lang="scss" scoped>
.consultation-container {
    margin: 0 auto;
    max-width: 1200px;
    width: 100%;
    display: flex;
    gap: 20px;
    padding: 20px;
    flex-wrap: wrap;
    min-height: calc(100vh - 120px);

    .sidebar {
        width: 320px;
        flex: 0 0 auto;

        @media (max-width: 768px) {
            width: 100%;
        }

        .ai-assistant-info {
            margin-bottom: 20px;
            background: linear-gradient(135deg,
                    var(--alpha-90) 0%,
                    var(--alpha-95) 100%);
            border-radius: 16px;
            padding: 16px;
            box-shadow:
                0 8px 32px var(--color-accent-wash),
                0 2px 8px var(--scrim-04);
            border: 1px solid var(--color-accent-wash);
            backdrop-filter: blur(10px);
            transition: all 0.3s ease;

            .breathing-circle {
                width: 60px;
                height: 60px;
                background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-text) 100%);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0 auto 12px;
                animation: breathing 4s ease-in-out infinite;
                box-shadow: 0 6px 24px var(--color-accent-wash-strong);
                position: relative;
            }

            .assistant-name {
                font-size: 16px;
                font-weight: 700;
                /* 对比度修正：渐变文字最浅处实测 2.24:1，brief 映射后的 accent 端仍只有 3.25:1，
                   而 16px/700 需要 4.5:1；改为单色 --color-accent-text（卡片底 4.80:1），
                   同时移除 background-clip:text 渐变裁切（否则颜色仍由渐变决定）。 */
                color: var(--color-accent-text);
                text-align: center;
                margin: 0 0 12px;
            }

            .online-status {
                display: flex;
                align-items: center;
                justify-content: center;
                color: var(--color-success);
                font-size: 12px;
                font-weight: 600;

                .status-dot {
                    width: 8px;
                    height: 8px;
                    background: var(--color-success);
                    border-radius: 50%;
                    margin-right: 8px;
                    animation: pulse 2s infinite;
                    box-shadow: 0 0 8px var(--color-success-wash-strong);
                }
            }
        }

        .session-history {
            background: var(--color-surface);
            border-radius: 16px;
            padding: 16px;
            box-shadow: 0 2px 12px var(--scrim-10);
            margin-bottom: 20px;
            min-height: 250px;
            display: flex;
            flex-direction: column;

            .section-title {
                font-size: 16px;
                font-weight: 600;
                color: var(--color-text);
                margin: 0 0 16px;
                display: flex;
                align-items: center;
                justify-content: space-between;
            }

            .session-list {
                overflow-y: auto;
                max-height: 200px;
                scrollbar-width: thin;
                scrollbar-color: var(--color-info-wash-strong) transparent;

                .session-item {
                    position: relative;
                    display: flex;
                    align-items: flex-start;
                    gap: 12px;
                    padding: 12px;
                    margin-bottom: 8px;
                    border-radius: 12px;
                    cursor: pointer;
                    transition: all 0.3s ease;
                    border: 2px solid transparent;

                    &:hover {
                        background: var(--color-primary-wash);
                        border-color: var(--color-primary-light);
                    }

                    &.active {
                        background: var(--color-primary-light);
                        border-color: var(--color-primary);
                    }

                    .session-info {
                        flex: 1;

                        .session-title {
                            font-weight: 500;
                            font-size: 14px;
                            color: var(--color-text);
                            margin-bottom: 4px;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                            text-align: left;

                            .session-meta {
                                display: flex;
                                align-items: center;
                                gap: 8px;
                                margin-bottom: 6px;

                                .session-time {
                                    font-size: 12px;
                                    color: var(--color-text-secondary);
                                }
                            }

                            .session-preview {
                                width: 200px;
                                font-size: 12px;
                                color: var(--color-text-secondary);
                                margin-bottom: 6px;
                                white-space: nowrap;
                                overflow: hidden;
                                text-overflow: ellipsis;
                            }

                            .session-stats {
                                display: flex;
                                align-items: center;
                                gap: 12px;

                                span {
                                    font-size: 12px;
                                    /* 对比度修正：brief 映射的 --color-text-placeholder 在白卡上只有
                                       2.47:1（< 4.5:1，旧值本身也只有 2.85:1）；这四处都是正文元信息
                                       （时间/条数/时长）而非表单占位符，改用 --color-text-secondary = 5.29:1。 */
                                    color: var(--color-text-secondary);
                                    display: flex;
                                    align-items: center;
                                    gap: 4px;
                                }
                            }
                        }

                        .session-actions {
                            position: absolute;
                            top: 10px;
                            right: 12px;
                        }
                    }
                }

                .no-sessions-text {
                    text-align: center;
                    font-size: 14px;
                    color: var(--color-text-secondary);
                }
            }
        }

        .emotion-garden {
            background: var(--color-bg);
            border-radius: 20px;
            padding: 16px;
            margin-bottom: 20px;
            box-shadow: 0 8px 32px var(--color-accent-light);
            border: 1px solid var(--alpha-20);
            position: relative;
            overflow: hidden;
            min-height: 300px;

            .garden-header {
                display: flex;
                align-items: center;
                justify-content: space-between;
                margin-bottom: 20px;
                position: relative;
                z-index: 2;

                .garden-title {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    font-size: 16px;
                    font-weight: 600;
                    color: var(--color-accent-text);
                }
            }

            .emotion-info {
                margin: 0 auto;
                width: 80px;
                height: 80px;
                border-radius: 50%;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                z-index: 10;
                box-shadow: 0 4px 16px var(--scrim-10);
                border: 2px solid var(--alpha-80);
                /* 对比度修正：brief 的 accent → accent-light 渐变上白字实测 1.36:1 ~ 3.27:1（15px/600 与
                   14px/700 需 4.5:1）；改用同族深阶单色 --color-accent-text = 4.84:1。 */
                background: var(--color-accent-text);
                color: var(--color-text-inverse);

                .emotion-name {
                    font-size: 15px;
                    font-weight: 600;
                    line-height: 1;
                    margin-bottom: 2px;
                }

                .emotion-score {
                    font-size: 14px;
                    font-weight: 700;
                    /* 原 opacity: 0.9 会把白字压成 90% 合成色，在主色深阶底上只有 4.25:1（< 4.5:1）；移除 */
                }
            }

            .warm-tips {
                text-align: center;
                margin-bottom: 16px;

                .emotion-status-text {
                    margin-bottom: 12px;

                    .status-label {
                        font-size: 14px;
                        color: var(--color-text-secondary);
                        margin-right: 8px;
                    }

                    .status-emotion {
                        font-size: 16px;
                        font-weight: 600;
                        padding: 4px 12px;
                        border-radius: 16px;
                        display: inline-block;
                    }
                }

                .emotion-intensity {
                    margin-bottom: 16px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 8px;

                    .intensity-dots {
                        display: flex;
                        gap: 4px;

                        .dot {
                            width: 8px;
                            height: 8px;
                            border-radius: 50%;
                            background: var(--color-border);
                            transition: all 0.3s ease;

                            &.active {
                                /* 状态指示点：brief 的 accent → accent-light 渐变对卡片底只有 3.10:1 → 1.03:1
                                   （非文字状态信息需 ≥3:1），改用 --color-accent-text：对卡片底 4.57:1、
                                   对熄灭态圆点 4.07:1。 */
                                background: var(--color-accent-text);
                                transform: scale(1.2);
                                box-shadow: 0 2px 8px var(--color-accent-wash-strong);
                            }
                        }
                    }

                    .intensity-text {
                        font-size: 12px;
                        color: var(--color-text-secondary);
                        font-weight: 500;
                    }
                }

                .warm-suggestion {
                    background: linear-gradient(135deg,
                            var(--alpha-95),
                            var(--alpha-80));
                    border-radius: 16px;
                    padding: 12px;
                    margin-bottom: 16px;
                    display: flex;
                    align-items: flex-start;
                    gap: 10px;
                    border: 1px solid var(--alpha-60);
                    box-shadow: 0 6px 20px var(--scrim-08);

                    .suggestion-icon {
                        font-size: 20px;
                        flex-shrink: 0;
                        margin-top: 2px;
                    }

                    .suggestion-content {
                        text-align: left;
                        flex: 1;

                        .suggestion-title {
                            font-size: 14px;
                            font-weight: 600;
                            color: var(--color-text-secondary);
                            margin-bottom: 6px;
                        }

                        .suggestion-text {
                            font-size: 13px;
                            color: var(--color-text-secondary);
                            line-height: 1.5;
                        }
                    }
                }

                .healing-actions {
                    margin-bottom: 16px;

                    .actions-title {
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: 8px;
                        font-size: 14px;
                        font-weight: 600;
                        color: var(--color-text-secondary);
                        margin-bottom: 16px;
                    }

                    .actions-list {
                        display: flex;
                        flex-direction: column;
                        gap: 10px;

                        .action-item {
                            background: linear-gradient(135deg,
                                    var(--alpha-90),
                                    var(--alpha-70));
                            border-radius: 12px;
                            padding: 12px;
                            display: flex;
                            align-items: center;
                            gap: 10px;
                            border: 1px solid var(--alpha-50);
                            box-shadow: 0 4px 12px var(--scrim-06);
                            text-align: left;

                            .action-icon {
                                font-size: 14px;
                                color: var(--color-accent-text);
                                flex-shrink: 0;
                            }

                            .action-text {
                                font-size: 12px;
                                color: var(--color-text-secondary);
                                line-height: 1.4;
                                flex: 1;
                            }
                        }
                    }
                }

                .risk-notice {
                    background: var(--color-accent-light);
                    border-radius: 16px;
                    padding: 16px;
                    display: flex;
                    align-items: flex-start;
                    gap: 12px;
                    border: 1px solid var(--color-accent-wash-strong);
                    box-shadow: 0 6px 20px var(--color-accent-wash);

                    .notice-icon {
                        font-size: 20px;
                        flex-shrink: 0;
                        margin-top: 2px;
                    }

                    .notice-content {
                        flex: 1;

                        .notice-title {
                            font-size: 14px;
                            font-weight: 600;
                            /* 对比度修正：brief 指定的 --color-accent-text 落在 --color-accent-light 上
                               实测 4.31:1（< 4.5:1），改用同族深阶 --color-warning = 4.87:1 */
                            color: var(--color-warning);
                            margin-bottom: 6px;
                        }

                        .notice-text {
                            font-size: 13px;
                            color: var(--color-warning);
                            line-height: 1.5;
                        }
                    }
                }
            }
        }
    }

    .chat-main {
        background: linear-gradient(135deg,
                var(--alpha-95) 0%,
                var(--alpha-98) 100%);
        border-radius: 20px;
        box-shadow:
            0 12px 40px var(--color-accent-wash),
            0 4px 16px var(--scrim-04);
        border: 1px solid var(--color-accent-wash);
        backdrop-filter: blur(10px);
        display: flex;
        flex-direction: column;
        overflow: hidden;
        flex: 1;
        min-width: 300px;
        min-height: 0;
        max-height: 800px;
        .chat-header {
            /* 对比度修正：brief 的 accent → accent-text 渐变上白字最浅端仅 3.27:1，
               而 14px 副标题需要 4.5:1；改用该渐变的深端单色 --color-accent-text（4.84:1，整幅一致）。 */
            background: var(--color-accent-text);
            color: var(--color-text-inverse);
            padding: 20px 24px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            position: relative;
            flex-shrink: 0;

            .header-left {
                display: flex;
                align-items: center;

                .chat-avatar {
                    width: 48px;
                    height: 48px;
                    background: var(--alpha-25);
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    margin-right: 16px;
                    box-shadow: 0 4px 12px var(--scrim-10);
                    position: relative;
                    z-index: 1;
                }

                .chat-info {
                    display: flex;
                    flex-direction: column;
                    align-items: start;

                    h2 {
                        font-size: 20px;
                        font-weight: 700;
                        margin-bottom: 4px;
                        color: var(--color-text-inverse);
                    }

                    p {
                        font-size: 14px;
                    }
                }
            }
        }

        .chat-messages {
            flex: 1;
            overflow-y: auto;
            padding: 24px;
            display: flex;
            flex-direction: column;
            gap: 16px;
            background: linear-gradient(135deg,
                    var(--alpha-02) 0%,
                    var(--alpha-05) 100%);
            min-height: 0;
            scrollbar-width: thin;
            scrollbar-color: var(--color-accent-wash-strong) transparent;
            .message-item {
                display: flex;
                align-items: flex-start;
                gap: 12px;

                .message-avatar {
                    width: 32px;
                    height: 32px;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 14px;
                    color: var(--color-text-inverse);
                    flex-shrink: 0;
                }

                /* 头像内只有 el-image（图标图片），无文字节点：其底色属装饰，不参与 3:1 文字要求 */
                &.ai-message {
                    .message-avatar {
                        background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-text) 100%);
                        box-shadow: 0 4px 12px var(--color-accent-wash-strong);
                    }
                }

                &.user-message {
                    .message-avatar {
                        background: var(--color-text-secondary);
                        box-shadow: 0 4px 12px var(--scrim-30);
                    }

                    /* 用户气泡：主色底 + inverse 字。多带一层 .message-content 提高特异性，
                       以压过下面 .message-bubble 的基础（AI）规则——同特异性时源码顺序会反超。 */
                    .message-content .message-bubble {
                        background: var(--color-primary);
                        color: var(--color-text-inverse);
                    }
                }

                .message-content {
                    max-width: 70%;

                    /* 气泡配色约定（本任务产出，Task 10 咨询记录页复用）：
                       基础 = AI 气泡（surface 底 + border 描边 + text 字），用户气泡在 &.user-message 中覆盖。 */
                    .message-bubble {
                        background: var(--color-surface);
                        border: 1px solid var(--color-border);
                        color: var(--color-text);
                        border-radius: 16px;
                        padding: 12px 16px;
                        position: relative;
                        animation: fadeInUp 0.4s ease-out;
                        box-shadow: 0 4px 16px var(--color-accent-wash);

                        p {
                            font-size: 14px;
                            line-height: 1.5;
                            text-align: left;
                        }

                        .typing-indicator {
                            display: flex;
                            gap: 4px;
                            padding: 8px 0;

                            .typing-dot {
                                width: 8px;
                                height: 8px;
                                /* 对比度修正：brief 指定的 --color-text-placeholder 在气泡白底上仅
                                   2.47:1（< 3:1），而“AI 正在输入”是有状态含义的非文字信息；
                                   改用 --color-text-secondary = 5.29:1。 */
                                background: var(--color-text-secondary);
                                border-radius: 50%;
                                animation: typing 1.5s ease-in-out infinite;

                                &:nth-child(2) {
                                    animation-delay: 0.2s;
                                }

                                &:nth-child(3) {
                                    animation-delay: 0.4s;
                                }
                            }
                        }

                        .streaming-content {
                            white-space: pre-wrap;
                            font-size: 15px;
                            line-height: 1.8;
                            text-align: left;
                        }

                        .cursor-blink {
                            animation: blink 1s step-end infinite;
                            color: var(--color-primary);
                            font-weight: bold;
                        }

                        @keyframes blink {
                            50% { opacity: 0; }
                        }

                        /* 错误消息样式。这四个红色字面量不在 brief 的四组映射表内（见报告 §2 差异），
                           按语义色令牌归并：底 --color-danger-light、描边 --color-danger
                           （对底 3.85:1，满足非文字 3:1）；文字用 --color-text = 10.08:1，
                           因为 --color-danger 作文字落在 --color-danger-light 上只有 3.85:1（< 4.5:1）。 */
                        .error-message {
                            background: var(--color-danger-light);
                            border: 1px solid var(--color-danger);
                            border-radius: 12px;
                            padding: 12px 16px;
                            color: var(--color-text);
                            font-weight: 500;
                            display: flex;
                            align-items: center;
                            gap: 8px;
                        }
                    }

                    .message-time {
                        font-size: 12px;
                        text-align: right;
                        color: var(--color-text-secondary);
                        margin-top: 4px;
                    }
                }
            }
        }

        .chat-input {
            border-top: 1px solid var(--color-accent-wash);
            padding: 20px 24px;
            display: flex;
            gap: 12px;
            align-items: flex-end;
            background: linear-gradient(135deg,
                    var(--alpha-50) 0%,
                    var(--alpha-70) 100%);
            backdrop-filter: blur(10px);
            flex-shrink: 0;

            .input-container {
                flex: 1;
            }

            .input-footer {
                display: flex;
                justify-content: space-between;
                align-items: center;
                font-size: 12px;
                color: var(--color-text-secondary);
                font-weight: 500;
            }

            .send-btn {
                height: 60px;
                width: 60px;
                border-radius: 16px;
                /* 按钮内只有图标、没有文字，属非文字的功能性元素 → 门槛 3:1。
                   本渐变的浅端 --color-accent 为 3.2733:1、中点 3.9692:1、深端 4.8373:1，全段达标。
                   注意：只有"控件内含小于 24px 的文字且文字为浅色"时才适用 4.5:1。
                   两条强制覆盖（background / border 的 important 声明）已按 brief 删除：本选择器
                   特异性 (0,4,1) 远高于 element-plus 的 .el-button--primary (0,1,0)，无需强制覆盖。 */
                background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-text) 100%);
                border: none;
                box-shadow: 0 6px 20px var(--color-accent-wash-strong);
                transition: all 0.3s ease;
            }
        }
    }
}
</style>
