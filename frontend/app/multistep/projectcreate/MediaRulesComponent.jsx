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
            <h3>Select Media Rules</h3>
            <p>We need to know what media rules this project will have.  Please select the relevant media rules.</p>
            <table>
                <tbody>
                <tr>
                    <td>
                        <input id="deep" type="radio" name="rules" checked={this.state.deep_archive} onChange={event=>this.setDeepArchive(event.target.value==='on')}/>
                        <label htmlFor="deep" style={{display: "inline", marginLeft: "0.4em"}}>Deep Archive</label>
                        <br />
                        The project will be archived to an external system after completion.
                    </td>
                </tr>
                <tr>
                    <td>
                        <input id="deletable" type="radio" name="rules" checked={this.state.deletable} onChange={event=>this.setDeepArchive(event.target.value!=='on')}/>
                        <label htmlFor="deletable" style={{display: "inline", marginLeft: "0.4em"}}>Deletable</label>
                        <br />
                        The project can be deleted by automated systems.
                    </td>
                </tr>
                <tr>
                    <td>
                        <input id="sensitive" type="checkbox" name="sensitive" checked={this.state.sensitive} onChange={event=>this.setState({sensitive: event.target.checked})}/>
                        <label htmlFor="sensitive" style={{display: "inline", marginLeft: "0.4em"}}>Sensitive</label>
                        <br />
                        The project will contain sensitive content.
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default MediaRulesComponent;