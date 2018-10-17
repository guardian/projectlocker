import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';

class MediaRulesComponent extends CommonMultistepComponent {
    static propTypes = {
        valueWasSet: PropTypes.func.isRequired,
        deletable: PropTypes.bool.isRequired,
        deep_archive: PropTypes.bool.isRequired,
        sensitive: PropTypes.bool.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            deletable: false,
            deep_archive: true,
            sensitive: false
        }
    }

    componentWillMount(){
        this.setState({deletable: this.props.deletable, deep_archive: this.props.deep_archive, sensitive: this.props.sensitive})
    }

    setDeepArchive(enabled){
        this.setState({deep_archive: enabled, deletable: !enabled})
    }

    render(){
        return <div>
            <h3>Select Media Management Rules</h3>
            <p>We need to know which archive management rules to apply to this project's media.</p>
            <table>
                <tbody>
                <tr>
                    <td>
                        <input id="deep" type="radio" name="rules" checked={this.state.deep_archive} onChange={event=>this.setDeepArchive(event.target.value==='on')}/>
                        <label htmlFor="deep" style={{display: "inline", marginLeft: "0.4em"}}>Keep media forever</label>
                        <p className="option-description">
                        The project and its assets will be archived long term externally after the project is marked as complete.
                        </p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <input id="deletable" type="radio" name="rules" checked={this.state.deletable} onChange={event=>this.setDeepArchive(event.target.value!=='on')}/>
                        <label htmlFor="deletable" style={{display: "inline", marginLeft: "0.4em"}}>Media can be removed once the projects has been completed</label>
                        <p className="option-description">
                        The project and its assets can be deleted after completion (Make sure all project deliverables are done before marking the project as complete).
                        </p>
                    </td>
                </tr>
                <tr>
                    <td>
                        <input id="sensitive" type="checkbox" name="sensitive" checked={this.state.sensitive} onChange={event=>this.setState({sensitive: event.target.checked})}/>
                        <label htmlFor="sensitive" style={{display: "inline", marginLeft: "0.4em"}}>Media should not leave the building</label>
                        <p className="option-description">
                        The project will contain sensitive content and all assets will be kept and archived within the Guardian.
                        </p>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default MediaRulesComponent;
