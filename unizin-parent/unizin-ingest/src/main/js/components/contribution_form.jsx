import React from 'react'
import FileUpload from './file_upload.jsx'
import styles from './contribution_form.scss'
import classNames from 'classnames'

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
            terms: false
        }
        Object.assign(this, linkFactory(this))
    }

    isFormValid() {
        const { title, terms} = this.state
        return (terms === true
            && title.trim().length > 0)
    }

    onSubmit(e) {
        e.preventDefault()
        const { title, description, terms} = this.state
        this.props.onSubmit({ title, description, terms})
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

        const inlineClasses = classNames("field", "inline")
        const requiredClasses = classNames("required", styles.supportingText)

        return (
          <div className="container">
            <form ref="form" onSubmit={::this.onSubmit} className={styles.contribute}>
                <h1>Contribution form</h1>
                <div className={styles.supportingText}>
                  Please provide some additional information about the file you are contributing.
                </div>
                <div className={requiredClasses}>
                  All fields are required
                </div>

                {this.renderFiles()}


                <div className="field">
                  <label htmlFor="title">Title</label>
                  <input valueLink={this.valueLink('title')} type="text" id="title" />
                </div>

                <div className="field">
                  <label htmlFor="description">Description</label>
                  <textarea valueLink={this.valueLink('description')} id="description" />
                </div>

                <div className={inlineClasses}>
                  <input checkedLink={this.checkedLink('terms')} type="checkbox" id="terms" />
                  <label htmlFor="terms">
                      I agree to the following the terms and conditions of Content Contribution
                  </label>

                  <ol className={styles.supportingText}>
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
                </div>

                <div className="actions">
                  <input type="submit" disabled={!this.isFormValid()} value="Contribute" className="primary"/>
                  <button onClick={::this.onCancel}>
                      Cancel
                  </button>
                </div>

            </form>
          </div>
        )
    }
}
