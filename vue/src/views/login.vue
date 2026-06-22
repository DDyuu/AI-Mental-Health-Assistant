<template>
    <div class="container">
        <!-- 登录标题容器 -->
        <div class="header">
            <div class="backHome">
                <el-icon>
                    <Back />
                </el-icon>
                <router-link to="/">返回首页</router-link>
            </div>
            <div class="title">
                <h2>登录您的账户</h2>
                <p>请输入您的登录信息</p>
            </div>
        </div>
        <!-- 登录表单容器 -->
        <div class="form-container">
            <el-form
                ref="ruleFormRef"
                :model="formData"
                :rules="rules"
                label-position="top"
            >
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="formData.username" size="large" placeholder="请输入用户名或邮箱" />
                </el-form-item>
                <el-form-item label="密码" prop="password">
                    <el-input v-model="formData.password" size="large" type="password" show-password placeholder="请输入密码"  @keyup.enter="submitForm" />
                </el-form-item>
                <el-button type="primary" @click="submitForm" class="btn">登录</el-button>
            </el-form>

            
            <div class="footer">
                <p>还没有账户?<router-link to="/auth/register">去注册</router-link></p>
            </div>
        </div>
    </div>
</template>

<script setup>
import { Back } from '@element-plus/icons-vue'
import { ref, reactive } from 'vue'
import { login } from '@/api/admin'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'



const ruleFormRef = ref(null)

const formData = reactive({
    username: '',
    password: ''
})
const rules = reactive({
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
    ]
})

const router = useRouter()

const submitForm = async () => {
    if(!ruleFormRef.value) return
    try {
        await ruleFormRef.value.validate()
        const res = await login(formData)
        //登录失败
        if(!res?.token){
            return ElMessage.error('登录失败，请检查用户名或密码')
        }
        //登录成功
        const userStore = useUserStore()
        userStore.login(res.userInfo, res.token)
        //根据用户角色决定跳转路径
        if(res.userInfo.userType === 2){
            router.push('/back/dashboard')
        }else if(res.userInfo.userType === 1){
            //用户角色为普通用户，跳转到首页
            router.push('/')
        }
            
    } catch (error) {
        ElMessage.error('校验失败', error)
    }
}
    
</script>

<style lang="scss" scoped>
.container {
    margin-top: 30px;
    width: 384px;

    .header {

        .backHome {
            margin-bottom: 60px;
            display: flex;
            flex-direction: row;
            align-items: center;
            justify-content: start;
        }

        .title {
            text-align: center;
            h2{
                font-size: 36px;
                margin-bottom: 10px;
                font-weight: bold;
            }
            p{
                font-size: 18px;
            }
        }
    }

    .form-container {

        .btn {
            margin-top: 40px;
            width: 100%;
        }

        .footer {
            margin-top: 20px;
            text-align: end;
        }
    }
}
</style>
