<template>
    <el-form v-model="formData" ref="ruleFormRef">
        <el-row :gutter="24">
            <template v-for="item in formItemAttrs" :key="item.prop">
                <el-col v-bind="item.col">
                    <el-form-item :label="item.label" :prop="item.prop">
                        <component v-model="formData[item.prop]" :is="isComp(item.comp)"
                            :placeholder="item.placeholder">
                            <template v-if="item.comp === 'select'">
                                <el-option label="全部" value="" />
                                <el-option v-for="option in item.options" :key="option.value" :label="option.label"
                                    :value="option.value" />
                            </template>
                        </component>
                    </el-form-item>
                </el-col>
            </template>
        </el-row>
        <el-row>
            <el-button type="primary" @click="handleSearch(formData)">查询</el-button>
            <el-button @click="handleReset(ruleFormRef)">重置</el-button>
        </el-row>
    </el-form>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'

const formData = reactive({})
const ruleFormRef = ref()


const props = defineProps({
    formItem: {
        type: Array,
        default: () => []
    }
})

const isComp = (comp) => {
    return {
        input: 'el-input',
        select: 'el-select'
    }[comp]
}

const handleSearch = (formData) => {
    emit('search', formData)
}

const handleReset = (formEl) => {
    if(!formEl) return
    formEl.resetFields()
}

const emit = defineEmits(['search', 'reset'])

const formItemAttrs = computed(() => {
    const { formItem } = props
    formItem.forEach(item => {
        item.col = { xs: 24, sm: 12, md: 8, lg: 6, xl: 6, }
    })
    return formItem
})
</script>
