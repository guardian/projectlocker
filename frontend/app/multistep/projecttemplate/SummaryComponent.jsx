import React from 'react';
import PropTypes from 'prop-types';
import ProjectTypeView from '../../EntryViews/ProjectTypeView.jsx';
import FileEntryView from '../../EntryViews/FileEntryView.jsx';
import PlutoSubtypeEntryView from '../../EntryViews/PlutoSubtypeEntryView.jsx';

class SummaryComponent extends React.Component {
    static propTypes = {
        fileId: PropTypes.number.isRequired,
        projectType: PropTypes.number.isRequired,
        name: PropTypes.string.isRequired,
        plutoSubtype: PropTypes.number,
        deprecated: PropTypes.bool.isRequired
    };

    constructor(props){
        super(props);
    }

    render() {
        return <table>
            <tbody>
            <tr>
                <td>Template name</td>
                <td id="template-name">{this.props.name}</td>
            </tr>
            <tr>
                <td>File</td>
                <td id="fileId"><FileEntryView entryId={this.props.fileId}/></td>
            </tr>
            <tr>
                <td>Pluto subtype</td>
                <td><PlutoSubtypeEntryView entryId={this.props.plutoSubtype}/></td>
            </tr>
            <tr>
                <td>Project type</td>
                <td id="projectType"><ProjectTypeView entryId={this.props.projectType}/></td>
            </tr>
            <tr>
                <td>Deprecated</td>
                <td id="deprecated"><span style={{display: this.props.deprecated ? "inline-block":"none"}}>Yes</span><span style={{display: !this.props.deprecated ? "inline-block":"none"}}>No</span></td>
            </tr>
            </tbody>
        </table>;
    }
}

export default SummaryComponent;