import service from '@/utils/request'
// 注册
export function register(data) {
  return service.post('/user/add', data)
}

export function startSession(data) {
    return service.post('psychological-chat/session/start', data)
}

export function getSessionList(params) {
    return service.get('psychological-chat/my-sessions', { params })
}

export function deleteSession(sessionId) {
    return service.delete(`psychological-chat/session/${sessionId}`)
}

export function getSessionDetail(sessionId) {
    return service.get(`psychological-chat/sessions/${sessionId}/messages`)
}

export function getSessionEmotion(sessionId) {
    return service.get(`psychological-chat/sessions/${sessionId}/emotion`)
}

export function addEmotionDiary(data) {
  return service.post('/emotion-diary', data)
}

//获取知识列表
export function getKnowledgeList(params) {
  return service.get(`/knowledge/article/page`, { params })
}
//获取知识文章详情
export function getKnowledgeDetail(articleId) {
  return service.get(`/knowledge/article/${articleId}`)
}