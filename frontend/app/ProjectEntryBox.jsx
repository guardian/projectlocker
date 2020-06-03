import React from 'react';
import PropTypes from 'prop-types';
import CommissionEntryView from "./EntryViews/CommissionEntryView.jsx";
import WorkingGroupEntryView from "./EntryViews/WorkingGroupEntryView.jsx";
import ProjectTypeView from "./EntryViews/ProjectTypeView.jsx";

class ProjectEntryBox extends React.Component {
    static propTypes = {
        rowKey: PropTypes.number.isRequired,   //standard react disambiguation key
        interfaceSize: PropTypes.number.isRequired,  //0 for large, 1 for small
        projectId: PropTypes.number.isRequired,    //project entry id
        projectTypeId: PropTypes.number.isRequired,
        projectTitle: PropTypes.string.isRequired,
        projectOwner: PropTypes.string.isRequired,
        commissionId: PropTypes.number,
        workingGroupId: PropTypes.number
    }

    render(){
        return <div key={this.props.rowKey} className={this.props.interfaceSize===0 ? "project_box_div_version" : "project_box_div_version_small"}>
            <div className="project_box_info_container">
                <div className="project_box_info_filter"/>
                <div className="project_box_info_icon"><i className="fa fa-id-card-o" style={{color: "darkgray", fontSize: "1.6em"}}/></div>
                <div className="project_box_info_label"><ProjectTypeView entryId={this.props.projectTypeId} /></div>

                <div className="project_box_info_filter"/>
                <div className="project_box_info_icon"><img src="/assets/images/project.png" alt="project" className="project_box_icon_normal"/></div>
                <div className="project_box_info_label">{this.props.projectTitle}</div>

                <div className="project_box_info_filter clickable"><i className="fa fa-search-plus" style={{marginLeft: "0.5em"}}/></div>
                <div className="project_box_info_icon "><img src="/assets/images/commission.png" alt="commission" className="project_box_icon_normal"/></div>
                <div className="project_box_info_label"><CommissionEntryView entryId={this.props.commissionId}/></div>

                <div className="project_box_info_filter clickable"><i className="fa fa-search-plus" style={{marginLeft: "0.5em"}}/></div>
                <div className="project_box_info_icon"><i className="fa fa-users" style={{color: "green", fontSize: "1.6em"}}/></div>
                <div className="project_box_info_label"><WorkingGroupEntryView entryId={this.props.workingGroupId}/></div>

                <div className="project_box_info_filter clickable"><i className="fa fa-search-plus" style={{marginLeft: "0.5em"}}/></div>
                <div className="project_box_info_icon"><i className="fa fa-user" style={{color: "black", fontSize: "1.6em"}}/></div>
                <div className="project_box_info_label">{this.props.projectOwner}</div>
            </div>
            <div className="project_box_open_buttons_container">
                <div className="project_box_button clickable"><img width="60" src="/assets/images/folder.png"/></div>
                <div className="project_box_button clickable"><img width="60" src="/assets/images/premiere_pro.png"/></div>
            </div>
        </div>
    }
}

export default ProjectEntryBox;