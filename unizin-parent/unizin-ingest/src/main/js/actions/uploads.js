import { httpPOST, json } from './utils'

export const SET_BATCH_ID = 'SET_BATCH_ID'

export function getBatchId() {
    return (dispatch) => {
        httpPOST('/nuxeo/api/v1/upload/').then(json).then(({batchId}) => {
            dispatch({
                type: SET_BATCH_ID,
                payload: batchId
            })
        })
    }
}
