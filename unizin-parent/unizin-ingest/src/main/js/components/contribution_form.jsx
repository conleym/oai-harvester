import React from 'react'
import FileUpload from './file_upload.jsx'
import styles from './contribution_form.scss'
const DEFAULT_LICENSE = "[select license]"

// https://facebook.github.io/react/docs/two-way-binding-helpers.html
// Because we use ES6 classes we can't use mixins, but I can build the functions
// and patch them onto the component.
const linkFactory = (component) => {
    return {
        valueLink: (key) => ({
            value: component.state[key],
            requestChange(value) {
                component.setState({ [key]: value })
            }
        }),
        checkedLink: (key) => ({
            checked: component.state[key] === true,
            requestChange(value) {
                component.setState({ [key]: value })
            }
        })
    }
}

export default class ContributionForm extends React.Component {
    static displayName = 'ContributionForm'

    static propTypes = {
        files: React.PropTypes.object.isRequired,
        onSubmit: React.PropTypes.func.isRequired,
        onCancel: React.PropTypes.func.isRequired,
    }

    constructor(props, context) {
        super(props, context)

        this.state = {
            title: '',
            terms: false,
            license: DEFAULT_LICENSE
        }
        Object.assign(this, linkFactory(this))
    }

    isFormValid() {
        const { title, terms, license } = this.state
        return (terms === true
            && license !== DEFAULT_LICENSE
            && title.trim().length > 0)
    }

    onSubmit(e) {
        e.preventDefault()
        const { title, description, terms, license } = this.state
        this.props.onSubmit({ title, description, terms, license })
    }

    onCancel(e) {
        e.preventDefault()
        this.props.onCancel()
    }

    renderFiles() {
        const { files } = this.props

        return Object.keys(files).map((key) => {
            const data = files[key]

            return (
                <FileUpload
                    key={key}
                    name={data.name}
                    size={data.size}
                    thumbnail={data.thumbnail}
                    progress={data.progress}
                    error={data.error} />
            )
        })
    }

    render() {

        return (
            <form ref="form" onSubmit={::this.onSubmit} className={styles.contribute}>
                <p>
                    Contribution Information
                </p>

                {this.renderFiles()}

                <label htmlFor="title">Title</label>
                <input valueLink={this.valueLink('title')} type="text" id="title" />

                <label htmlFor="description">Description</label>
                <textarea valueLink={this.valueLink('description')} id="description" />

                <label htmlFor="license">Licensing</label>
                <select valueLink={this.valueLink('license')} id="license">
                    <option value={DEFAULT_LICENSE}>Select License</option>
                    <option value="A">Apple</option>
                    <option value="B">Banana</option>
                    <option value="C">Cranberry</option>
                </select>

                <input checkedLink={this.checkedLink('terms')} type="checkbox" id="terms" />
                <label htmlFor="terms">
                    I agree to the following the terms and conditions of Content Contribution
                </label>

                <ol>
                    <li>
                        You have the right to submit the content you submit
                        under these terms.
                    </li>
                    <li>
                        Your submission under these terms does not knowingly
                        infringe anyone’s legal rights.
                    </li>
                    <li>
                        You authorize Internet2 and Unizin to use, copy,
                        distribute, publicly perform and display, make
                        available, preserve, retain, and to make derivative
                        translations or accessible versions of the content you
                        submit for noncommercial purposes.
                    </li>
                    <li>
                        You authorize Internet2 and Unizin to use your name and
                        other bibliographic information in association with the
                        content you submit.
                    </li>
                    <li>
                        You agree that Internet2 and Unizin have no obligations
                        to use, host, or retain the content you submit.
                    </li>
                </ol>

                <input type="submit" disabled={!this.isFormValid()}>
                    Contribute
                </input>

                <button onClick={::this.onCancel}>
                    Cancel
                </button>

            </form>

        )
    }
}
