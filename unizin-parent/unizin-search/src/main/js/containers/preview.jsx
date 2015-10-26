import React from 'react'
import CopyContent from './copy_content'
import { routePreviewUrl } from '../actions/route.js'

export default class Insert extends React.Component {
    static displayName = 'Insert'

    static propTypes = {
        history: React.PropTypes.shape({
            goBack: React.PropTypes.func.isRequired
        }).isRequired,
        params: React.PropTypes.shape({
            uid: React.PropTypes.string.isRequired
        }).isRequired
    }

    render() {
        const { uid } = this.props.params

        const done = (document) => {
            window.location = routePreviewUrl(document).url
        }

        return (
            <CopyContent
                uid={uid}
                history={this.props.history}
                done={done} />
        )
    }
}
