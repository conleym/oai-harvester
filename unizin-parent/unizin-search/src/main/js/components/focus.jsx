import React from 'react'
import $ from '../jquery_tabbable.js'

/**
 * WARNING: This does *not* behave as a typical React component.
 *
 * When Focus mounts it will find the first tabbable element and focus it. If
 * you completely swap out the content it won't trigger a 2nd time. If you need
 * to swap the content and have it re-focus you should use <Focus key={...}
 */
export default class Focus extends React.Component {
    static displayName = 'Focus'

    static propTypes = {
        children: React.PropTypes.node.isRequired,
    }

    componentDidMount() {
        process.nextTick(() => {
            const DOM = React.findDOMNode(this)
            const first = $(DOM).find(':tabbable').first()
            first.focus()
        })
    }

    render() {
        return this.props.children
    }
}
