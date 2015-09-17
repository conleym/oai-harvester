import React from 'react'
import { connect } from 'react-redux'

class Result extends React.Component {
    static displayName = 'Result'

    render() {
        return (
            <div>
                <button onClick={this.props.history.goBack}>
                    &lt; Back to results
                </button>

                We don't have any actual data for this page yet.
            </div>
        )
    }
}

function mapStateToProps(state, props) {
    return {}
}

export default connect(
  mapStateToProps,
  { }
)(Result)
