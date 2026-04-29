// composables/useCurrentChannel.js
export const useCurrentChannel = () => {
    const state = useState('currentChannel', () => ({
        name: '全部',
        id: 0
    }))
    return state
}