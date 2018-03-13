import React from 'react';
import PropTypes from 'prop-types';
import StorageEntryView from '../../EntryViews/StorageEntryView.jsx';
import WorkingGroupEntryView from '../../EntryViews/WorkingGroupEntryView.jsx';
import CommissionEntryView from '../../EntryViews/CommissionEntryView.jsx';
import ProjectTemplateEntryView from '../../EntryViews/ProjectTemplateEntryView.jsx';

class SummaryComponent extends React.Component {
    static propTypes = {
        projectTemplates: PropTypes.array.isRequired,
        selectedProjectTemplate: PropTypes.number.isRequired,
        storages: PropTypes.array.isRequired,
        selectedStorage: PropTypes.number.isRequired,
        projectName: PropTypes.string.isRequired,
        projectFilename: PropTypes.string.isRequired,
        wgList: PropTypes.array.isRequired,
        selectedWorkingGroupId: PropTypes.number.isRequired,
        selectedCommissionId: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);
    }

    render() {
        return <table>
            <tbody>
            <tr>
                <td>New project name</td>
                <td id="project-name">{this.props.projectName}</td>
            </tr>
            <tr>
                <td>New file name</td>
                <td id="project-name">{this.props.projectFilename}</td>
            </tr>
            <tr>
                <td>Project template</td>
                <td id="project-template-id"><ProjectTemplateEntryView entryId={this.props.selectedProjectTemplate}/></td>
            </tr>
            <tr>
                <td>Storage</td>
                <td id="storage"><StorageEntryView entryId={this.props.selectedStorage}/></td>
            </tr>
            <tr>
                <td>Working group</td>
                <td id="working-group"><WorkingGroupEntryView entryId={this.props.selectedWorkingGroupId}/></td>
            </tr>
            <tr>
                <td>Commission</td>
                <td id="commission"><CommissionEntryView entryId={this.props.selectedCommissionId}/></td>
            </tr>
            </tbody>
        </table>;
    }
}

export default SummaryComponent;