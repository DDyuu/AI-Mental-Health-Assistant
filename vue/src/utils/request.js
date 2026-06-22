import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
    baseURL: '/api',
    timeout: 30000
})

// 请求拦截器 - 自动携带 Token
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Token = token
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器 - 统一处理
service.interceptors.response.use(
    (response) => {
        //处理响应数据
        const {data, config} = response
        //处理业务状态码
        if(data.code === 200){
            return data.data
        } else{
            if(data.code === -1){
                //登录过期
                if(!config.url.includes('/auth/login')){
                    ElMessage.error(data.msg || '登录过期，请重新登录')
                    localStorage.removeItem('token')
                    localStorage.removeItem('userInfo')
                    window.location.href = '/auth/login'
                }
            } else {
                ElMessage.error(data.msg || '请求失败')
                return Promise.reject("网络请求失败....")
            }
        }
    },
    (error) => {
        //
        return Promise.reject(error)
    }
)


export default service
