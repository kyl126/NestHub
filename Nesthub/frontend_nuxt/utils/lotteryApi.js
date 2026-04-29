// utils/lotteryApi.js
import { getToken } from './auth'

/**
 * 获取请求头
 */
const getHeaders = () => {
    const token = getToken()  // 使用 getToken 函数
    return {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
    }
}

/**
 * 获取奖品列表
 */
export async function getPrizes() {
    const config = useRuntimeConfig()
    const response = await fetch(`${config.public.apiBaseUrl}/api/lottery/prizes`, {
        headers: getHeaders()
    })
    const data = await response.json()
    if (data.code === 200) {
        return data.data
    }
    throw new Error(data.message || '获取奖品失败')
}

/**
 * 检查抽奖资格
 */
export async function checkQualification(userId, activity, posts) {
    const config = useRuntimeConfig()
    const response = await fetch(
        `${config.public.apiBaseUrl}/api/lottery/qualification/check?userId=${userId}&activity=${activity}&posts=${posts}`,
        { headers: getHeaders() }
    )
    const data = await response.json()
    if (data.code === 200) {
        return data.data
    }
    throw new Error(data.message || '检查资格失败')
}

/**
 * 执行抽奖
 */
export async function drawLottery(userId, activity, posts) {
    const config = useRuntimeConfig()
    const response = await fetch(`${config.public.apiBaseUrl}/api/lottery/draw`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ userId, activityValue: activity, postValue: posts })
    })
    const data = await response.json()
    if (data.code === 200) {
        return data.data
    }
    throw new Error(data.message || '抽奖失败')
}

/**
 * 获取抽奖记录
 */
export async function getLotteryRecords(userId, limit = 10) {
    const config = useRuntimeConfig()
    const response = await fetch(
        `${config.public.apiBaseUrl}/api/lottery/records/${userId}?limit=${limit}`,
        { headers: getHeaders() }
    )
    const data = await response.json()
    if (data.code === 200) {
        return data.data
    }
    throw new Error(data.message || '获取记录失败')
}