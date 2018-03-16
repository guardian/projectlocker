import React from 'react';
import ShowPasswordComponent from '../ShowPasswordComponent.jsx';
import PropTypes from 'prop-types';
import PostrunActionList from '../postrun/PostrunActionList.jsx';

class SummaryComponent extends React.Component {
    static propTypes = {
        name: PropTypes.string.isRequired,
        opensWith: PropTypes.string.isRequired,
        version: PropTypes.string.isRequired,
        fileExtension: PropTypes.string.isRequired,
        plutoType: PropTypes.string,
        plutoSubtype: PropTypes.string,
        postrunActions: PropTypes.array.isRequired,
        selectedPostruns: PropTypes.array.isRequired
    };

    constructor(props){
        super(props);
    }

    render() {
        return <table>
            <tbody>
            <tr>
                <td>Name</td>
                <td id="projectTypeName">{this.props.name}</td>
            </tr>
            <tr>
                <td>Opens With</td>
                <td id="projectTypeOpensWith">{this.props.opensWith}</td>
            </tr>
            <tr>
                <td>Required Version</td>
                <td id="projectTypeRequiredVersion">{this.props.version}</td>
            </tr>
            <tr>
                <td>File extension</td>
                <td id="projectTypeFileExtension">{this.props.fileExtension}</td>
            </tr>
            <tr>
                <td>Pluto type identifier</td>
                <td id="plutoType">{this.props.plutoType}</td>
            </tr>
            <tr>
                <td>Pluto subtype identifier</td>
                <td id="plutoSubtype">{this.props.plutoSubtype}</td>
            </tr>
            <tr>
                <td>Postrun actions</td>
                <td><PostrunActionList actionList={this.props.postrunActions} selectedActions={this.props.selectedPostruns}/></td>
            </tr>
            </tbody>
        </table>;
    }
}

export default SummaryComponent;