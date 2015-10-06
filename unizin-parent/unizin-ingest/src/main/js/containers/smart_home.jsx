import React from 'react'
import { connect } from 'react-redux';
import { Link } from 'react-router'

const { object } = React.PropTypes

class SmartHome extends React.Component {
    static displayName = 'SmartHome'

    static propTypes = {
        query: object.isRequired
    }

    render() {
        return (
            <div>
                Hello World
                <pre>{JSON.stringify(this.props.query, null, 2)}</pre>

                <Link to="/">Foo</Link>
                <br/>
                <Link to="/">Bar</Link>
            </div>
        )
    }
}

function mapStateToProps(state) {
    return {
        query: state.router.location.query
    }
}

export default connect(
  mapStateToProps,
  { }
)(SmartHome)
