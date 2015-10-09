import React from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import Loader from '../components/loading.jsx'
import invariant from 'react/lib/invariant'

const nonce = Math.floor(Math.random() * 16777215).toString(16)
// I can't use a symbol here because they don't work as top level keys for React
// props.
const SMART_LOADER = 'SMART_LOADER'  + nonce
const DISPATCH = 'DISPATCH'  + nonce


const propsChanged = (before = {}, after = {}) => {
    // YAY! Value comparison!
    return (JSON.stringify(before) != JSON.stringify(after))
}

class SmartLoader extends React.Component {
    static displayName = 'SmartLoader'

    static propTypes = {
        [SMART_LOADER]: React.PropTypes.shape({
            Component: React.PropTypes.constructor,
            loading: React.PropTypes.any,
            params: React.PropTypes.object,
        })
    }

    componentDidMount() {
        const { [SMART_LOADER]: smartLoader } = this.props
        smartLoader.loader(this.props[DISPATCH], smartLoader.params, {})
    }

    componentDidUpdate(prevProps, prevState) {
        const { [SMART_LOADER]: smartLoader } = this.props
        const { params: lastParams = {} } = prevProps[SMART_LOADER]
        const { params: nextParams = {} } = this.props[SMART_LOADER]
        if (propsChanged(lastParams, nextParams)) {
            smartLoader.loader(this.props[DISPATCH], nextParams, lastParams)
        }
    }

    render() {
        const { [SMART_LOADER]: smartLoader, ...props} = this.props

        if (smartLoader.loading) {
            return (
                <Loader />
            )
        }

        return (<smartLoader.Component {...props} />)
    }
}

const connectLoader = (spec, mapState, mapDispatch) => (Component) => {
    const {
        inputFilter, // (state, props) => params
        isReady = (params, state) => true,
        loader = (dispatch, params, lastParams) => undefined,
    } = spec

    if (process.env.NODE_ENV !== 'production') {
        invariant(inputFilter, 'inputFilter is required')
    } else {
        invariant(inputFilter)
    }

    function mapStateToProps(state, props) {
        const params = inputFilter(state, props)
        if (process.env.NODE_ENV !== 'production') {
            const testParams = inputFilter(state, props)
            invariant(!propsChanged(params, testParams),
                'loader must be pure')
        }
        if (isReady(params, state) === true) {
            return {
                ...mapState(state, props),
                [SMART_LOADER]: {
                    Component,
                    loader,
                    params,
                }
            }
        }
        return {
            [SMART_LOADER]: {
                loader,
                params,
                loading: true
            }
        }
    }

    return connect(
        mapStateToProps,
        function(dispatch, props) {
            let tmp
            if (typeof mapDispatch === 'function') {
                tmp = mapDispatch(dispatch, props)
            } else {
                tmp = bindActionCreators(mapDispatch, dispatch)
            }

            tmp[DISPATCH] = dispatch
            return tmp
        }
    )(SmartLoader)
}

export default connectLoader
