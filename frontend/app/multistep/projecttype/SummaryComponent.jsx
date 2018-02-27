import React from 'react';
import ShowPasswordComponent from '../ShowPasswordComponent.jsx';

class SummaryComponent extends React.Component {
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
            </tbody>
        </table>;
    }
}

export default SummaryComponent;