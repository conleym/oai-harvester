import React from 'react'
import setupDropzone from '../components/dropzone'
import Home from '../components/home'
import { connect } from 'react-redux'

const { object } = React.PropTypes

class SmartHome extends React.Component {
    static displayName = 'SmartHome'

    static propTypes = {
        query: object.isRequired
    }

    componentWillMount() {
        const { Dropzone, Files, clickFileInput } = setupDropzone()
        this.Dropzone = Dropzone
        this.Files = Files
        this.clickFileInput = clickFileInput
    }

    render() {
        return (
            <this.Dropzone>
                <Home
                    Files={this.Files}
                    onSelectFile={this.clickFileInput}
                    query={this.props.query} />
            </this.Dropzone>
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
