<template>
    <div>
        <el-dialog :title="isEdit ? '编辑文章' : '新增文章'" v-model="dialogVisible" width="50%" @close="handleClose">
            <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px">
                <el-form-item label="文章标题" prop="title">
                    <el-input v-model="formData.title" placeholder="请输入文章标题" maxlength="50" show-word-limit />
                </el-form-item>
                <el-form-item label="分类" prop="categoryId">
                    <el-select v-model="formData.categoryId" placeholder="请选择分类">
                        <el-option v-for="item in props.categories" :key="item.id" :label="item.categoryName"
                            :value="item.id" />
                    </el-select>
                </el-form-item>
                <el-form-item label="摘要" prop="summary">
                    <el-input v-model="formData.summary" placeholder="请输入摘要(可选)" maxlength="200" show-word-limit
                        type="textarea" rows="4" />
                </el-form-item>
                <el-form-item label="标签" prop="tags">
                    <el-select v-model="formData.tagArray" placeholder="请选择标签" multiple filterable allow-create
                        style="width: 100%">
                        <el-option v-for="item in commonTags" :key="item" :label="item" :value="item" />
                    </el-select>
                </el-form-item>
                <el-form-item label="封面图片">
                    <div class="cover-upload">
                        <el-upload class="cover-uploader" :before-upload="beforeUpload"
                            :http-request="handleUploadRequest" accept="image/*" :show-file-list="false">
                            <div v-if="!imgUrl" class="cover-placeholder">
                                <p>点击上传封面</p>
                            </div>
                            <div v-else class="cover-image">
                                <img :src="imgUrl" alt="封面图片" />
                            </div>
                        </el-upload>
                        <div v-if="imgUrl" style="display:flex; justify-self: start;">
                            <el-button type="danger" @click="handleRemoveCover" size="small">移除封面</el-button>
                        </div>
                    </div>
                </el-form-item>
                <el-form-item label="文章内容" prop="content">
                    <div style="border: 1px solid #dcdfe6; width: 100%;">
                        <Toolbar :editor="editorRef" :defaultConfig="toolbarConfig"
                            style="border-bottom: 1px solid #dcdfe6;" />
                        <Editor v-model="contentHtml" :defaultConfig="editorConfig"
                            style="height: 400px; overflow-y: auto;" @onCreated="handleCreated" maxlength="5000"
                            show-word-limit />
                    </div>
                </el-form-item>
            </el-form>
            <!-- 预览文章内容 -->
            <div v-if="btnPreview" class="w-e-text"
                style="padding: 10px 20px; border: 1px solid #dcdfe6; border-radius: 4px; min-height: 100px;">
                <p style="font-size: 16px; font-weight: bold; justify-self: start;">预览内容</p>
                <div v-html="formData.content"></div>
            </div>
            <template #footer>
                <el-button @click="btnPreview = !btnPreview">{{ btnPreview ? '关闭预览' : '预览效果' }}</el-button>
                <el-button @click="handleClose">取消</el-button>
                <el-button type="primary" @click="handleSubmit" :loading="loading">{{ isEdit ? '更新' : '创建'
                    }}</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, computed, shallowRef, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile, createArticle } from '@/api/admin'
import { fileBaseUrl } from '@/config/index'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const props = defineProps({
    modelValue: {
        type: Boolean,
        default: false
    },
    categories: {
        type: Array,
        default: () => []
    },
    article: {
        type: Object,
        default: null
    }
})

const emit = defineEmits(['update:modelValue', 'success'])

const dialogVisible = computed({
    get() {
        return props.modelValue
    },
    set(val) {
        emit('update:modelValue', val)
    }
})

const isEdit = computed(() => {
    return !!props.article?.id
})

//监听编辑数据
watch(() => props.article, (newVal) => {
    if (newVal) {
        nextTick(() => {
            Object.assign(formData, newVal)
            // 同步富文本内容到编辑器
            contentHtml.value = formData.content
            //使用现有的id
            businessId.value = newVal.id
            //封面url
            imgUrl.value = fileBaseUrl + newVal.coverImage
        })

    }
})

const handleClose = () => {
    formRef.value.resetFields()
    businessId.value = null
    imgUrl.value = ''
    formData.tagArray = []
    emit('update:modelValue', false)
}

// wangeditor 编辑器配置
const editorRef = shallowRef()
const toolbarConfig = {
    toolbarKeys: [
        'headerSelect',
        'bold',
        'italic',
        'underline',
        'through',
        'color',
        'bgColor',
        '|',
        'fontSize',
        'fontFamily',
        'lineHeight',
        '|',
        'bulletedList',
        'numberedList',
        'justifyLeft',
        'justifyCenter',
        'justifyRight',
        '|',
        'insertImage',
        'insertLink',
        '|',
        'undo',
        'redo'
    ]
}
const editorConfig = {
    placeholder: '请输入文章内容...',
    maxLength: 5000,
    counter: {
        enable: true,
        maxLength: 5000
    }
    
}
const contentHtml = ref('')

watch(contentHtml, (val) => {
    formData.content = val
})

const handleCreated = (editor) => {
    editorRef.value = editor
}

onBeforeUnmount(() => {
    if (editorRef.value) {
        editorRef.value.destroy()
    }
})

// 表单数据
const formData = reactive({
    "title": '',
    "content": '',
    "coverImage": '',
    "categoryId": '',
    "summary": '',
    "tags": '',
    "id": ''
})

// 表单校验规则
const rules = reactive({
    title: [
        { required: true, message: '请输入文章标题', trigger: 'blur' },
        { max: 200, message: '文章标题最多200个字符', trigger: 'blur' }
    ],
    categoryId: [
        { required: true, message: '请选择分类', trigger: 'change' }
    ],
    content: [
        { required: true, message: '请输入文章内容', trigger: 'blur' }
    ]
})

const commonTags = [
    '情绪管理', '焦虑', '抑郁', '压力', '睡眠',
    '冥想', '正念', '放松', '心理健康', '自我成长',
    '人际关系', '工作压力', '学习方法', '生活技巧'
]

//上传封面
const imgUrl = ref('')
const beforeUpload = (file) => {
    //针对上传前的文件进行校验
    const isImage = file.type.startsWith('image/')
    const isLt2M = file.size / 1024 / 1024 < 2
    if (!isImage) {
        ElMessage.error('请上传图片文件!')
        return false
    }
    if (!isLt2M) {
        ElMessage.error('图片大小不能超过2MB!')
        return false
    }
    return true
}

//上传封面请求
const businessId = ref(null)
const handleUploadRequest = async ({ file }) => {
    //UUID生成
    businessId.value = crypto.randomUUID()
    const fileRes = await uploadFile(file, {
        businessId: businessId.value,
    })
    //拼接显示的图片
    imgUrl.value = fileBaseUrl + fileRes
    //储存相对路径（不带域名）
    formData.coverImage = fileRes
}

//删除封面
const handleRemoveCover = () => {
    imgUrl.value = ''
    formData.coverImage = ''
}

// 预览文章
const btnPreview = ref(false)

// 提交表单
const formRef = ref(null)
const loading = ref(false)
const handleSubmit = () => {
    formRef.value.validate((valid, fields) => {
        if (!valid) return

        loading.value = true
        const submitData = {
            ...formData,
            tags: formData.tagArray ? formData.tagArray.join(',') : ''
        }
        delete submitData.tagArray

        createArticle(submitData).then(res => {
            loading.value = false
            emit('success')
            dialogVisible.value = false
        })
    })
}
</script>

<style lang="scss" scoped>
.cover-placeholder {
    width: 200px;
    height: 120px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #8b949e;
    background: #f6f8fa;
}

.cover-image {
    width: 200px;
    height: 120px;
    display: flex;

}

:deep(.w-e-text-container) {
    text-align: left;
}
</style>