import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';

class MetadataComponent extends CommonMultistepComponent {
    static propTypes = {
        title: PropTypes.string.isRequired,
        description: PropTypes.string.isRequired,
        runnable: PropTypes.string.isRequired,
        version: PropTypes.number.isRequired,
        valueWasSet: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);
        console.log(props);
        this.state = {
            title: props.title,
            description: props.description,
            initialLoad: true
        }
    }

    render(){
        return <div>
            <h3>Postrun Information</h3>
            <p>Please describe what the function of this postrun script is</p>
            <table>
                <tbody>
                <tr>
                    <td>Action Name</td>
                    <td><input id="title-input" value={this.props.title} onChange={event=>this.props.valueWasSet({title: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>Description</td>
                    <td><textarea id="description-input"
                                  onChange={event=>this.props.valueWasSet({description: event.target.value})}
                                  value={this.props.description}/>
                    </td>
                </tr>
                <tr>
                    <td>Executable name</td>
                    <td><b>{this.props.runnable}</b></td>
                </tr>
                <tr>
                    <td>Version</td>
                    <td><b>{this.props.version}</b></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default MetadataComponent;