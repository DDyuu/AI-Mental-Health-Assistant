<template>
  <div>
    <div>
    <PageHead title="知识文章">
      <template #buttons>
        <el-button type="primary" @click="handleDialog()">新增</el-button>
      </template>
    </PageHead>
    <TableSearch :formItem="formItem" @search="handleSearch" />
    </div>

    <div>
      <el-table :data="tableData" style="width: 100%; margin-top: 25px;" >
        <el-table-column  label="标题" show-overflow-tooltip fixed="left">
          <template #default="scope">
            <div style="display: flex; align-items: center">
              <el-icon><Timer /></el-icon>
              <span>{{ scope.row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column  label="分类" width="150">
          <template #default="scope">
            <div style="display: flex; align-items: center">
              <span>{{ scope.row.categoryName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="150" />
        <el-table-column prop="readCount" label="阅读量" width="150" />
        <el-table-column label="发布时间" width="150" show-overflow-tooltip>
          <template #default="scope">
            <span>{{ formatDate(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
          <el-button text type="primary" @click="handleDialog(scope.row)">编辑</el-button>
          <el-button @click="handlePublish(scope.row)" v-if="scope.row.status === 0 || scope.row.status === 2" text type="success">发布</el-button>
          <el-button @click="handleUnpublish(scope.row)" v-if="scope.row.status === 1" text type="warning">下架</el-button>
          <el-button @click="handleDelete(scope.row)" text type="danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div>
      <el-pagination
        layout="total, prev, next"
        :total="pagination.total"
        :page-size="pagination.pageSize"
        :current-page.sync="pagination.currentPage"
        @current-change="handleChange"
      />
    </div>

    <div>
      <ArticleDialog v-model:modelValue="dialogVisible" :categories="categories" @success="handleSuccess" :article="currentArticle" />
    </div>
    
  </div>
</template>

<script setup>
import PageHead from '@/components/PageHead.vue'
import TableSearch from '@/components/TableSearch.vue'
import { categoryTree, articlePage, getArticleDetail, updateArticle, deleteArticle } from '@/api/admin'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ArticleDialog from '@/components/articleDialog.vue'

const formItem = [
  { comp: 'input', prop: 'title', label: '标题', placeholder: '请输入文章标题' },
  { comp: 'select', prop: 'category', label: '分类', placeholder: '请选择分类' },
  {
    comp: 'select', prop: 'status', label: '状态', placeholder: '请选择状态', options: [
      { label: '草稿', value: '0' },
      { label: '已发布', value: '1' },
      { label: '已下架', value: '2' }
    ]
  }
]

const handleChange = (val) => {
  pagination.currentPage = val
  handleSearch()
}

// 编辑文章
const dialogVisible = ref(false)

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const handleSearch = async (formData) => {
  const params = {
    ...pagination,
    ...formData
  }
  const { records, total } = await articlePage(params)
  tableData.value = records
  pagination.total = total
}

const categoryMap = reactive({})

const categories = ref([])

const tableData = ref([])

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = (n) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

onMounted(async () => {
  const data = await categoryTree()

  categories.value = data
  formItem[1].options = data.map(item => {
    categoryMap[item.id] = item.categoryName
    return {
      label: item.categoryName,
      value: item.id
    }
  })

  //获取列表
  handleSearch()
})

const handleSuccess = () => {
  ElMessage.success('新增或更新成功')
  handleSearch()
}

const currentArticle = ref({})
const handleDialog = (row) => {
  if (!row || !row.id) {
    currentArticle.value = null
    dialogVisible.value = true
    return
  }
  getArticleDetail(row.id).then(res => {
    currentArticle.value = res
    dialogVisible.value = true
  })
}

//发布文章与下架
const handlePublish = (row) => {
  ElMessageBox.confirm(`确认发布文章${row.title}吗？`, '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => {
    // 发布文章逻辑
    updateArticle(row.id, 1).then(res => {
      ElMessage.success('发布成功')
      handleSearch()
    })
  })
}
const handleUnpublish = (row) => {
  ElMessageBox.confirm(`确认下架文章${row.title}吗？`, '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 下架文章逻辑
    // 调用下架接口
    updateArticle(row.id, 2).then(res => {
      ElMessage.success('下架成功')
      handleSearch()
    })
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除文章${row.title}吗？`, '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'danger'
  }).then(() => {
    // 删除文章逻辑
    deleteArticle(row.id).then(res => {
      ElMessage.success('删除成功')
      handleSearch()
    })
  })
}
</script>
