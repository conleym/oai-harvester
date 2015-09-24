import { history } from '../actions/route.js'
export const ROUTE = Symbol('route')


export default store => next => action => {
    if (action[ROUTE] == null) {
        return next(action)
    }
    console.log('route', action)
    const { route, query } = action[ROUTE]

    history.pushState(null, route, query)
    next(action)
}
